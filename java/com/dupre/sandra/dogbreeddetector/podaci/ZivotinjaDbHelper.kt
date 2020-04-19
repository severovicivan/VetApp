package com.dupre.sandra.dogbreeddetector.podaci

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

import com.dupre.sandra.dogbreeddetector.podaci.ZivotinjaContract.ZivotinjaEntry

class ZivotinjaDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_IME, null, DATABASE_VERZIJA) {

    override fun onCreate(db: SQLiteDatabase) {
        val SQL_CREATE_ZIVOTINJE_TABLE = ("CREATE TABLE " + ZivotinjaEntry.TABLE_NAME + "("
                + ZivotinjaEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ZivotinjaEntry.COLUMN_LJUBIMAC_IME + " TEXT NOT NULL, "
                + ZivotinjaEntry.COLUMN_LJUBIMAC_PASMINA + " TEXT, "
                + ZivotinjaEntry.COLUMN_LJUBIMAC_SPOL + " INTEGER NOT NULL, "
                + ZivotinjaEntry.COLUMN_LJUBIMAC_TEZINA + " INTEGER NOT NULL DEFAULT 0);")

        db.execSQL(SQL_CREATE_ZIVOTINJE_TABLE)
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, i: Int, i1: Int) {

    }

    companion object {

        //Ime datoteke baze
        private val DATABASE_IME = "azil.db"

        //Verzija baze. Ukoliko se promijeni shema, mora se uvećati vrijednost verzije
        private val DATABASE_VERZIJA = 1
    }
}