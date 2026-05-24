import { NextResponse } from 'next/server';
import { query } from '@/lib/db';

export async function POST(request) {
  try {
    const { image } = await request.json();
    const apiKey = process.env.GEMINI_API_KEY;

    // Hard fail if no API key configured
    if (!apiKey || apiKey.trim() === '' || apiKey === 'MY_GEMINI_API_KEY') {
      return NextResponse.json(
        { error: 'GEMINI_API_KEY no está configurada en el servidor.' },
        { status: 500 }
      );
    }

    // Hard fail if no image provided
    if (!image) {
      return NextResponse.json(
        { error: 'No se proporcionó ninguna imagen para analizar.' },
        { status: 400 }
      );
    }

    // --- Call Gemini API ---
    const mimeMatch = image.match(/^data:(image\/[\w+.-]+);base64,/);
    const mimeType = mimeMatch ? mimeMatch[1] : 'image/jpeg';
    const cleanBase64 = image.replace(/^data:image\/[\w+.-]+;base64,/, '');

    const prompt = `
      Analiza esta imagen de un ticket/factura/recibo de compras para el hogar.
      Extrae el nombre de la tienda o comercio, el importe total pagado, la categoría (debe ser uno de estos valores exactos: 'Alimentos', 'Servicio', 'Alquiler', 'Diverso') y una lista de los artículos comprados con su nombre, cantidad y precio unitario estimado.
      
      Devuelve únicamente un objeto JSON con la siguiente estructura exacta:
      {
        "comercio": "Nombre de la Tienda",
        "total": 123.45,
        "categoria": "Alimentos",
        "articulos": [
          {"nombre": "Nombre de Producto", "cantidad": 2.0, "precio": 1.50}
        ]
      }
      
      No devuelvas bloques de código markdown, solo el texto del JSON limpio sin rodeos.
    `;

    const payload = {
      contents: [
        {
          parts: [
            { text: prompt },
            {
              inlineData: {
                mimeType: mimeType,
                data: cleanBase64,
              },
            },
          ],
        },
      ],
    };

    const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${apiKey}`;
    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      const errBody = await response.text();
      throw new Error(`Gemini API error ${response.status}: ${errBody}`);
    }

    const resBody = await response.json();

    if (!resBody.candidates || resBody.candidates.length === 0) {
      throw new Error('Gemini no devolvió candidatos en la respuesta.');
    }

    let rawText = resBody.candidates[0].content.parts[0].text.trim();

    // Robust JSON extraction — strip any surrounding markdown or prose
    const jsonStart = rawText.indexOf('{');
    const jsonEnd = rawText.lastIndexOf('}');
    if (jsonStart === -1 || jsonEnd === -1) {
      throw new Error(`Gemini no devolvió JSON válido. Respuesta: ${rawText.substring(0, 300)}`);
    }
    rawText = rawText.substring(jsonStart, jsonEnd + 1);

    const data = JSON.parse(rawText);
    const extraction = {
      storeName: data.comercio || 'Comercio Desconocido',
      totalAmount: Number(data.total) || 0.0,
      category: data.categoria || 'Diverso',
      items: (data.articulos || []).map((item) => ({
        name: item.nombre || 'Producto',
        quantity: Number(item.cantidad) || 1.0,
        price: Number(item.precio) || 0.0,
      })),
    };

    // --- Persist the Extracted Data in the Database ---

    // 1. Add as Variable Expense
    const expenseTitle = `Compra en ${extraction.storeName}`;
    await query(
      'INSERT INTO expenses (title, amount, category, timestamp, is_recurring, paid_by) VALUES ($1, $2, $3, $4, $5, $6)',
      [expenseTitle, extraction.totalAmount, extraction.category, Date.now(), false, 'Milton (Scan Web)']
    );

    // 2. Upsert items into inventory
    for (const item of extraction.items) {
      const invRes = await query('SELECT * FROM inventory_items WHERE LOWER(name) = LOWER($1)', [item.name]);

      if (invRes.rows.length > 0) {
        const invItem = invRes.rows[0];
        const newStock = Number(invItem.current_stock) + item.quantity;
        const currentBestPrice = invItem.best_price;
        const isBestPrice = currentBestPrice === null || item.price < currentBestPrice;

        if (isBestPrice) {
          await query(
            `UPDATE inventory_items 
             SET current_stock = $1, second_best_price = $2, second_best_store = $3, best_price = $4, best_store = $5, last_updated = $6
             WHERE id = $7`,
            [newStock, currentBestPrice, invItem.best_store, item.price, extraction.storeName, Date.now(), invItem.id]
          );
        } else {
          await query(
            `UPDATE inventory_items 
             SET current_stock = $1, last_updated = $2
             WHERE id = $3`,
            [newStock, Date.now(), invItem.id]
          );
        }
      } else {
        await query(
          `INSERT INTO inventory_items (name, current_stock, min_stock_alert, unit, depletion_rate_per_day, best_store, best_price, last_updated)
           VALUES ($1, $2, $3, $4, $5, $6, $7, $8)`,
          [item.name, item.quantity, 1.0, 'u', 0.1, extraction.storeName, item.price, Date.now()]
        );
      }
    }

    return NextResponse.json(extraction);
  } catch (error) {
    console.error('Scan error:', error);
    return NextResponse.json({ error: error.message }, { status: 500 });
  }
}
