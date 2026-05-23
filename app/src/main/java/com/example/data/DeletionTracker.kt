package com.example.data

import android.content.Context
import android.content.SharedPreferences

object DeletionTracker {
    private const val PREFS_NAME = "deletion_tracker_prefs"
    private const val KEY_EXPENSES = "deleted_expenses_ids"
    private const val KEY_INVENTORY = "deleted_inventory_ids"
    private const val KEY_SHOPPING = "deleted_shopping_ids"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun trackExpenseDeletion(context: Context, id: Int) {
        val set = getPrefs(context).getStringSet(KEY_EXPENSES, emptySet())?.toMutableSet() ?: mutableSetOf()
        set.add(id.toString())
        getPrefs(context).edit().putStringSet(KEY_EXPENSES, set).apply()
    }

    fun trackInventoryDeletion(context: Context, id: Int) {
        val set = getPrefs(context).getStringSet(KEY_INVENTORY, emptySet())?.toMutableSet() ?: mutableSetOf()
        set.add(id.toString())
        getPrefs(context).edit().putStringSet(KEY_INVENTORY, set).apply()
    }

    fun trackShoppingDeletion(context: Context, id: Int) {
        val set = getPrefs(context).getStringSet(KEY_SHOPPING, emptySet())?.toMutableSet() ?: mutableSetOf()
        set.add(id.toString())
        getPrefs(context).edit().putStringSet(KEY_SHOPPING, set).apply()
    }

    fun getDeletedExpenses(context: Context): Set<Int> {
        return getPrefs(context).getStringSet(KEY_EXPENSES, emptySet())?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    fun getDeletedInventory(context: Context): Set<Int> {
        return getPrefs(context).getStringSet(KEY_INVENTORY, emptySet())?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    fun getDeletedShopping(context: Context): Set<Int> {
        return getPrefs(context).getStringSet(KEY_SHOPPING, emptySet())?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    fun clearDeletedExpenses(context: Context, ids: Set<Int>) {
        val current = getPrefs(context).getStringSet(KEY_EXPENSES, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.removeAll(ids.map { it.toString() }.toSet())
        getPrefs(context).edit().putStringSet(KEY_EXPENSES, current).apply()
    }

    fun clearDeletedInventory(context: Context, ids: Set<Int>) {
        val current = getPrefs(context).getStringSet(KEY_INVENTORY, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.removeAll(ids.map { it.toString() }.toSet())
        getPrefs(context).edit().putStringSet(KEY_INVENTORY, current).apply()
    }

    fun clearDeletedShopping(context: Context, ids: Set<Int>) {
        val current = getPrefs(context).getStringSet(KEY_SHOPPING, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.removeAll(ids.map { it.toString() }.toSet())
        getPrefs(context).edit().putStringSet(KEY_SHOPPING, current).apply()
    }
}
