import { NextResponse } from 'next/server';

export async function POST(request) {
  try {
    const { images } = await request.json();
    const apiKey = process.env.GEMINI_API_KEY;

    // Hard fail if no API key configured
    if (!apiKey || apiKey.trim() === '' || apiKey === 'MY_GEMINI_API_KEY') {
      return NextResponse.json(
        { error: 'GEMINI_API_KEY no está configurada en el servidor.' },
        { status: 500 }
      );
    }

    // Hard fail if no images provided
    if (!images || images.length === 0) {
      return NextResponse.json(
        { error: 'No se proporcionaron imágenes para analizar.' },
        { status: 400 }
      );
    }

    const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${apiKey}`;

    // Clean base64 strings and prepare image parts
    const formattedParts = images.map((imgBase64) => {
      const mimeMatch = imgBase64.match(/^data:(image\/[\w+.-]+);base64,/);
      const mimeType = mimeMatch ? mimeMatch[1] : 'image/jpeg';
      const cleanBase64 = imgBase64.replace(/^data:image\/[\w+.-]+;base64,/, '');
      return {
        inlineData: {
          mimeType,
          data: cleanBase64,
        },
      };
    });

    // --- PASO 1: DETECCIÓN INICIAL ---
    const prompt1 = `
      Analiza detenidamente estas fotos de estantes de despensa/alacena.
      Detecta todos los productos que alcances a ver. Para cada uno, identifica su nombre genérico (ej. Arroz Blanco, Leche Entera, Atún en lata), su marca comercial y la cantidad que se estima visualmente en los estantes.
      
      Devuelve únicamente un objeto JSON con esta estructura exacta:
      {
        "productos": [
          {"nombre": "Nombre Genérico", "marca": "Marca", "cantidad": 2, "unidad": "u"}
        ]
      }
      
      No incluyas formateo markdown de bloques de código en tu respuesta, solo el JSON plano.
    `;

    const response1 = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        contents: [
          {
            parts: [{ text: prompt1 }, ...formattedParts],
          },
        ],
      }),
    });

    if (!response1.ok) {
      const errBody = await response1.text();
      throw new Error(`Gemini Paso 1 error ${response1.status}: ${errBody}`);
    }

    const resBody1 = await response1.json();

    if (!resBody1.candidates || resBody1.candidates.length === 0) {
      throw new Error('Gemini Paso 1 no devolvió candidatos.');
    }

    let rawText1 = resBody1.candidates[0].content.parts[0].text.trim();

    // Robust JSON extraction
    const jsonStart1 = rawText1.indexOf('{');
    const jsonEnd1 = rawText1.lastIndexOf('}');
    if (jsonStart1 === -1 || jsonEnd1 === -1) {
      throw new Error(`Paso 1: JSON inválido. Respuesta: ${rawText1.substring(0, 300)}`);
    }
    rawText1 = rawText1.substring(jsonStart1, jsonEnd1 + 1);

    // --- PASO 2: SEGUNDA REVISIÓN Y AUDITORÍA CONTRA DISTORSIÓN ---
    const prompt2 = `
      A partir de las imágenes de despensa provistas y de la siguiente lista de detección inicial de productos:
      ${rawText1}
      
      Realiza una segunda revisión crítica y rigurosa sobre las imágenes. Actúa como un auditor escrupuloso de almacén. Tu objetivo es prevenir la distorsión del inventario:
      1. Descarta empaques que parezcan vacíos o basura tirada.
      2. Corrige productos duplicados que puedan haber aparecido repetidos en fotos superpuestas o diferentes ángulos de los estantes.
      3. Corrige marcas comerciales mal interpretadas o nombres de productos poco realistas.
      
      Devuelve la lista final auditada en el mismo formato JSON exacto:
      {
        "productos": [
          {"nombre": "Nombre Genérico", "marca": "Marca", "cantidad": 2, "unidad": "u"}
        ]
      }
      
      No devuelvas bloques de código markdown, solo el JSON plano definitivo.
    `;

    const response2 = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        contents: [
          {
            parts: [{ text: prompt2 }, ...formattedParts],
          },
        ],
      }),
    });

    let productsList = [];

    if (!response2.ok) {
      // Fallback to step 1 results if step 2 API call fails
      console.warn(`Gemini Paso 2 falló (${response2.status}), usando resultados del Paso 1.`);
      const data1 = JSON.parse(rawText1);
      productsList = data1.productos || [];
    } else {
      const resBody2 = await response2.json();

      if (!resBody2.candidates || resBody2.candidates.length === 0) {
        // Fallback to step 1
        console.warn('Gemini Paso 2 sin candidatos, usando resultados del Paso 1.');
        const data1 = JSON.parse(rawText1);
        productsList = data1.productos || [];
      } else {
        let rawText2 = resBody2.candidates[0].content.parts[0].text.trim();

        // Robust JSON extraction
        const jsonStart2 = rawText2.indexOf('{');
        const jsonEnd2 = rawText2.lastIndexOf('}');
        if (jsonStart2 === -1 || jsonEnd2 === -1) {
          // Fallback to step 1 if step 2 JSON is malformed
          console.warn('Paso 2 JSON inválido, usando resultados del Paso 1.');
          const data1 = JSON.parse(rawText1);
          productsList = data1.productos || [];
        } else {
          rawText2 = rawText2.substring(jsonStart2, jsonEnd2 + 1);
          const data2 = JSON.parse(rawText2);
          productsList = data2.productos || [];
        }
      }
    }

    // Format products for UI validation staging list
    const validatedProductsList = productsList.map((prod, index) => ({
      tempId: `staging_${Date.now()}_${index}`,
      name: prod.nombre || 'Producto Desconocido',
      brand: prod.marca || 'Genérica',
      quantity: Number(prod.cantidad) || 1.0,
      unit: prod.unidad || 'u',
    }));

    return NextResponse.json({ products: validatedProductsList });
  } catch (error) {
    console.error('Pantry shelf scan error:', error);
    return NextResponse.json({ error: error.message }, { status: 500 });
  }
}
