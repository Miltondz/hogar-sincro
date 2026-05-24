import { NextResponse } from 'next/server';
import { query } from '@/lib/db';

export async function POST() {
  try {
    console.log("Iniciando borrado total de la base de datos de Neon Cloud...");
    await query('BEGIN');
    await query('DELETE FROM expenses');
    await query('DELETE FROM inventory_items');
    await query('DELETE FROM shopping_items');
    await query('COMMIT');
    console.log("Base de datos Neon Cloud borrada exitosamente!");
    return NextResponse.json({ success: true });
  } catch (error) {
    try {
      await query('ROLLBACK');
    } catch (e) {
      console.error('Error rolling back:', e);
    }
    console.error('Error clearing database:', error);
    return NextResponse.json({ error: error.message }, { status: 500 });
  }
}
