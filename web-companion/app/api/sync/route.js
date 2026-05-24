import { NextResponse } from 'next/server';
import { query } from '@/lib/db';

export async function POST(request) {
  try {
    const body = await request.json();
    const { expenses, inventory, shopping } = body;

    // Start SQL transaction
    await query('BEGIN');

    try {
      // 1. Sync Expenses
      if (expenses) {
        const { deletedIds, localItems } = expenses;

        // Perform deletions
        if (deletedIds && deletedIds.length > 0) {
          for (const id of deletedIds) {
            await query('DELETE FROM expenses WHERE id = $1', [id]);
          }
        }

        // Perform upserts
        if (localItems && localItems.length > 0) {
          const q = `
            INSERT INTO expenses (id, title, amount, category, timestamp, is_recurring, recurring_due_date, paid_by)
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
            ON CONFLICT (id) DO UPDATE SET
              title = EXCLUDED.title,
              amount = EXCLUDED.amount,
              category = EXCLUDED.category,
              timestamp = EXCLUDED.timestamp,
              is_recurring = EXCLUDED.is_recurring,
              recurring_due_date = EXCLUDED.recurring_due_date,
              paid_by = EXCLUDED.paid_by
          `;
          for (const item of localItems) {
            await query(q, [
              item.id,
              item.title || 'Gasto',
              Number(item.amount) || 0.0,
              item.category || 'Alimentos',
              Number(item.timestamp) || Date.now(),
              item.isRecurring || false,
              item.recurringDueDate || '',
              item.paidBy || 'Milton'
            ]);
          }
        }
      }

      // 2. Sync Inventory Items
      if (inventory) {
        const { deletedIds, localItems } = inventory;

        // Perform deletions
        if (deletedIds && deletedIds.length > 0) {
          for (const id of deletedIds) {
            await query('DELETE FROM inventory_items WHERE id = $1', [id]);
          }
        }

        // Perform upserts
        if (localItems && localItems.length > 0) {
          const q = `
            INSERT INTO inventory_items (id, name, current_stock, min_stock_alert, unit, depletion_rate_per_day, best_store, best_price, second_best_store, second_best_price, last_updated)
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
            ON CONFLICT (id) DO UPDATE SET
              name = EXCLUDED.name,
              current_stock = EXCLUDED.current_stock,
              min_stock_alert = EXCLUDED.min_stock_alert,
              unit = EXCLUDED.unit,
              depletion_rate_per_day = EXCLUDED.depletion_rate_per_day,
              best_store = EXCLUDED.best_store,
              best_price = EXCLUDED.best_price,
              second_best_store = EXCLUDED.second_best_store,
              second_best_price = EXCLUDED.second_best_price,
              last_updated = EXCLUDED.last_updated
          `;
          for (const item of localItems) {
            await query(q, [
              item.id,
              item.name || 'Artículo',
              Number(item.currentStock) || 0.0,
              Number(item.minStockAlert) || 0.0,
              item.unit || 'u',
              Number(item.depletionRatePerDay) || 0.0,
              item.bestStore || null,
              item.bestPrice ? Number(item.bestPrice) : null,
              item.secondBestStore || null,
              item.secondBestPrice ? Number(item.secondBestPrice) : null,
              Number(item.lastUpdated) || Date.now()
            ]);
          }
        }
      }

      // 3. Sync Shopping Items
      if (shopping) {
        const { deletedIds, localItems } = shopping;

        // Perform deletions
        if (deletedIds && deletedIds.length > 0) {
          for (const id of deletedIds) {
            await query('DELETE FROM shopping_items WHERE id = $1', [id]);
          }
        }

        // Perform upserts
        if (localItems && localItems.length > 0) {
          const q = `
            INSERT INTO shopping_items (id, product_name, quantity_to_buy, unit, estimated_price, is_bought, target_store)
            VALUES ($1, $2, $3, $4, $5, $6, $7)
            ON CONFLICT (id) DO UPDATE SET
              product_name = EXCLUDED.product_name,
              quantity_to_buy = EXCLUDED.quantity_to_buy,
              unit = EXCLUDED.unit,
              estimated_price = EXCLUDED.estimated_price,
              is_bought = EXCLUDED.is_bought,
              target_store = EXCLUDED.target_store
          `;
          for (const item of localItems) {
            await query(q, [
              item.id,
              item.productName || 'Artículo',
              Number(item.quantityToBuy) || 1.0,
              item.unit || 'u',
              Number(item.estimatedPrice) || 0.0,
              item.isBought || false,
              item.targetStore || null
            ]);
          }
        }
      }

      await query('COMMIT');
    } catch (txError) {
      await query('ROLLBACK');
      throw txError;
    }

    // 4. Fetch updated data from DB and map to camelCase for mobile clients
    const expensesRes = await query('SELECT * FROM expenses ORDER BY id ASC');
    const expensesList = expensesRes.rows.map(row => ({
      id: row.id,
      title: row.title,
      amount: row.amount,
      category: row.category,
      timestamp: Number(row.timestamp),
      isRecurring: row.is_recurring,
      recurringDueDate: row.recurring_due_date,
      paidBy: row.paid_by
    }));

    const inventoryRes = await query('SELECT * FROM inventory_items ORDER BY id ASC');
    const inventoryList = inventoryRes.rows.map(row => ({
      id: row.id,
      name: row.name,
      currentStock: row.current_stock,
      minStockAlert: row.min_stock_alert,
      unit: row.unit,
      depletionRatePerDay: row.depletion_rate_per_day,
      bestStore: row.best_store,
      bestPrice: row.best_price,
      secondBestStore: row.second_best_store,
      secondBestPrice: row.second_best_price,
      lastUpdated: Number(row.last_updated)
    }));

    const shoppingRes = await query('SELECT * FROM shopping_items ORDER BY id ASC');
    const shoppingList = shoppingRes.rows.map(row => ({
      id: row.id,
      productName: row.product_name,
      quantityToBuy: row.quantity_to_buy,
      unit: row.unit,
      estimatedPrice: row.estimated_price,
      isBought: row.is_bought,
      targetStore: row.target_store
    }));

    return NextResponse.json({
      success: true,
      expenses: expensesList,
      inventory: inventoryList,
      shopping: shoppingList
    });
  } catch (error) {
    console.error('Error in batch sync endpoint:', error);
    return NextResponse.json({ success: false, error: error.message }, { status: 500 });
  }
}
