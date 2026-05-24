import { NextResponse } from 'next/server';
import { query } from '@/lib/db';

export async function GET() {
  try {
    const result = await query('SELECT * FROM inventory_items ORDER BY name ASC');
    return NextResponse.json(result.rows);
  } catch (error) {
    console.error('Error fetching inventory:', error);
    return NextResponse.json({ error: error.message }, { status: 500 });
  }
}

export async function POST(request) {
  try {
    const body = await request.json();
    const {
      id,
      name,
      currentStock,
      minStockAlert,
      unit,
      depletionRatePerDay,
      bestStore,
      bestPrice,
      secondBestStore,
      secondBestPrice,
    } = body;

    const stock = Number(currentStock) || 0;
    const alertLimit = Number(minStockAlert) || 0;
    const rate = Number(depletionRatePerDay) || 0;
    const bPrice = bestPrice ? Number(bestPrice) : null;
    const sPrice = secondBestPrice ? Number(secondBestPrice) : null;
    const lastUpdated = Date.now();

    if (id) {
      // Update
      const q = `
        UPDATE inventory_items 
        SET name = $1, current_stock = $2, min_stock_alert = $3, unit = $4, depletion_rate_per_day = $5, 
            best_store = $6, best_price = $7, second_best_store = $8, second_best_price = $9, last_updated = $10
        WHERE id = $11
        RETURNING *
      `;
      const result = await query(q, [
        name,
        stock,
        alertLimit,
        unit,
        rate,
        bestStore || null,
        bPrice,
        secondBestStore || null,
        sPrice,
        lastUpdated,
        id,
      ]);
      return NextResponse.json(result.rows[0]);
    } else {
      // Insert
      const q = `
        INSERT INTO inventory_items (name, current_stock, min_stock_alert, unit, depletion_rate_per_day, best_store, best_price, second_best_store, second_best_price, last_updated)
        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
        RETURNING *
      `;
      const result = await query(q, [
        name,
        stock,
        alertLimit,
        unit,
        rate,
        bestStore || null,
        bPrice,
        secondBestStore || null,
        sPrice,
        lastUpdated,
      ]);
      return NextResponse.json(result.rows[0]);
    }
  } catch (error) {
    console.error('Error saving inventory item:', error);
    return NextResponse.json({ error: error.message }, { status: 500 });
  }
}

export async function DELETE(request) {
  try {
    const { searchParams } = new URL(request.url);
    const id = searchParams.get('id');

    if (!id) {
      return NextResponse.json({ error: 'Missing id parameter' }, { status: 400 });
    }

    await query('DELETE FROM inventory_items WHERE id = $1', [id]);
    return NextResponse.json({ success: true });
  } catch (error) {
    console.error('Error deleting inventory item:', error);
    return NextResponse.json({ error: error.message }, { status: 500 });
  }
}
