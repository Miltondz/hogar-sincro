const { Client } = require('pg');

async function main() {
  const connectionString = "postgresql://neondb_owner:npg_Nu0Yhmp1GrBP@ep-blue-water-aco8ck54-pooler.sa-east-1.aws.neon.tech/neondb?sslmode=require";
  const client = new Client({
    connectionString,
    ssl: { rejectUnauthorized: false }
  });

  try {
    await client.connect();
    console.log("Conectado exitosamente a Neon PostgreSQL...");
    
    console.log("Borrando gastos (expenses)...");
    await client.query("DELETE FROM expenses");
    
    console.log("Borrando inventario (inventory_items)...");
    await client.query("DELETE FROM inventory_items");
    
    console.log("Borrando lista de compras (shopping_items)...");
    await client.query("DELETE FROM shopping_items");
    
    console.log("¡Toda la base de datos de Neon Cloud ha sido borrada con éxito!");
  } catch (err) {
    console.error("Error limpiando Neon:", err);
  } finally {
    await client.end();
  }
}

main();
