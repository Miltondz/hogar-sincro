package com.example

import com.example.data.NeonDatabaseHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.sql.DriverManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testNeonConnectionAndTables() = runBlocking {
    println("--- START TEST NEON CONNECTION ---")
    try {
        Class.forName("org.postgresql.Driver")
        println("Driver organization loaded.")
        
        val url = "jdbc:postgresql://ep-blue-water-aco8ck54-pooler.sa-east-1.aws.neon.tech/neondb?sslmode=require&user=neondb_owner&password=npg_Nu0Yhmp1GrBP"
        val conn = DriverManager.getConnection(url)
        println("Connection established! AutoCommit default: ${conn.autoCommit}")
        
        val metadata = conn.metaData
        println("Database: ${metadata.databaseProductName} v${metadata.databaseProductVersion}")
        
        // Let's check tables
        val rs = metadata.getTables(null, null, "%", arrayOf("TABLE"))
        println("Existing Tables in Database:")
        var foundAny = false
        while (rs.next()) {
            val tableName = rs.getString("TABLE_NAME")
            val tableSchem = rs.getString("TABLE_SCHEM")
            println("Table found - Schema: $tableSchem, Name: $tableName")
            foundAny = true
        }
        rs.close()
        
        if (!foundAny) {
            println("No tables found in the database. Creating tables now...")
            val result = NeonDatabaseHelper.createTables()
            println("Create tables result: $result")
        } else {
            // Re-run createTables to make sure they are checked/updated
            val result = NeonDatabaseHelper.createTables()
            println("Verification check of table creation: $result")
        }
        
        conn.close()
        println("Connection closed successfully.")
    } catch (e: Exception) {
        println("FAILED to connect or execute queries. Error sequence:")
        e.printStackTrace()
        fail("JDBC Connection failed: ${e.message}")
    }
    println("--- END TEST NEON CONNECTION ---")
  }
}
