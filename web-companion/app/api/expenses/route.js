import { NextResponse } from 'next/server';
import { query } from '@/lib/db';

export async function GET() {
  try {
    const result = await query('SELECT * FROM expenses ORDER BY timestamp DESC');
    return NextResponse.json(result.rows);
  } catch (error) {
    console.error('Error fetching expenses:', error);
    return NextResponse.json({ error: error.message }, { status: 500 });
  }
}

export async function POST(request) {
  try {
    const body = await request.json();
    const { id, title, amount, category, timestamp, isRecurring, recurringDueDate, paidBy } = body;

    const ts = timestamp || Date.now();
    const isRec = isRecurring || false;
    const due = recurringDueDate || '';
    const payer = paidBy || 'Milton';

    if (id) {
      // Update
      const q = `
        UPDATE expenses 
        SET title = $1, amount = $2, category = $3, timestamp = $4, is_recurring = $5, recurring_due_date = $6, paid_by = $7
        WHERE id = $8
        RETURNING *
      `;
      const result = await query(q, [title, amount, category, ts, isRec, due, payer, id]);
      return NextResponse.json(result.rows[0]);
    } else {
      // Insert
      const q = `
        INSERT INTO expenses (title, amount, category, timestamp, is_recurring, recurring_due_date, paid_by)
        VALUES ($1, $2, $3, $4, $5, $6, $7)
        RETURNING *
      `;
      const result = await query(q, [title, amount, category, ts, isRec, due, payer]);
      return NextResponse.json(result.rows[0]);
    }
  } catch (error) {
    console.error('Error saving expense:', error);
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

    await query('DELETE FROM expenses WHERE id = $1', [id]);
    return NextResponse.json({ success: true });
  } catch (error) {
    console.error('Error deleting expense:', error);
    return NextResponse.json({ error: error.message }, { status: 500 });
  }
}
