import { NextResponse } from 'next/server';
import { query } from '@/lib/db';

export async function GET() {
  try {
    const result = await query('SELECT * FROM shopping_items ORDER BY is_bought ASC, product_name ASC');
    // Map database snake_case fields to frontend camelCase if needed, or handle in page
    return NextResponse.json(result.rows);
  } catch (error) {
    console.error('Error fetching shopping list:', error);
    return NextResponse.json({ error: error.message }, { status: 500 });
  }
}

export async function POST(request) {
  try {
    const body = await request.json();
    const { id, productName, quantityToBuy, unit, estimatedPrice, isBought, targetStore } = body;

    const qty = Number(quantityToBuy) || 1;
    const price = Number(estimatedPrice) || 0;
    const bought = isBought || false;

    // Check if we are marking a pending item as bought
    let transitionToBought = false;
    if (id && bought) {
      const currentRes = await query('SELECT * FROM shopping_items WHERE id = $1', [id]);
      if (currentRes.rows.length > 0 && !currentRes.rows[0].is_bought) {
        transitionToBought = true;
      }
    } else if (!id && bought) {
      transitionToBought = true;
    }

    let savedItem;
    if (id) {
      const q = `
        UPDATE shopping_items 
        SET product_name = $1, quantity_to_buy = $2, unit = $3, estimated_price = $4, is_bought = $5, target_store = $6
        WHERE id = $7
        RETURNING *
      `;
      const result = await query(q, [productName, qty, unit || 'u', price, bought, targetStore || null, id]);
      savedItem = result.rows[0];
    } else {
      const q = `
        INSERT INTO shopping_items (product_name, quantity_to_buy, unit, estimated_price, is_bought, target_store)
        VALUES ($1, $2, $3, $4, $5, $6)
        RETURNING *
      `;
      const result = await query(q, [productName, qty, unit || 'u', price, bought, targetStore || null]);
      savedItem = result.rows[0];
    }

    if (transitionToBought) {
      // 1. Instantly register this as a monthly variable expense!
      const expenseTitle = `${productName} (${targetStore || 'Compras Web'})`;
      const totalCost = price * qty;
      await query(
        'INSERT INTO expenses (title, amount, category, timestamp, is_recurring, paid_by) VALUES ($1, $2, $3, $4, $5, $6)',
        [expenseTitle, totalCost, 'Alimentos', Date.now(), false, 'Milton (Web)']
      );

      // 2. Refill the corresponding inventory item if exists!
      const invRes = await query('SELECT * FROM inventory_items WHERE LOWER(name) = LOWER($1)', [productName]);
      if (invRes.rows.length > 0) {
        const invItem = invRes.rows[0];
        const newStock = Number(invItem.current_stock) + qty;
        await query(
          'UPDATE inventory_items SET current_stock = $1, last_updated = $2 WHERE id = $3',
          [newStock, Date.now(), invItem.id]
        );
      }
    }

    return NextResponse.json(savedItem);
  } catch (error) {
    console.error('Error saving shopping item:', error);
    return NextResponse.json({ error: error.message }, { status: 500 });
  }
}

export async function DELETE(request) {
  try {
    const { searchParams } = new URL(request.url);
    const id = searchParams.get('id');
    const clearBought = searchParams.get('clearBought') === 'true';

    if (clearBought) {
      await query('DELETE FROM shopping_items WHERE is_bought = true');
      return NextResponse.json({ success: true });
    }

    if (!id) {
      return NextResponse.json({ error: 'Missing id or clearBought parameter' }, { status: 400 });
    }

    await query('DELETE FROM shopping_items WHERE id = $1', [id]);
    return NextResponse.json({ success: true });
  } catch (error) {
    console.error('Error deleting shopping item:', error);
    return NextResponse.json({ error: error.message }, { status: 500 });
  }
}
