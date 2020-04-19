package com.dupre.sandra.dogbreeddetector.podaci

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log

import com.dupre.sandra.dogbreeddetector.podaci.ZivotinjaContract.ZivotinjaEntry

/**
 * [ContentProvider] za VetApp.
 */
class ZivotinjaProvider : ContentProvider() {

    // Database helper objekt
    private var mDbHelper: ZivotinjaDbHelper? = null

    /**
     * Učitavanje pružatelja sadržaja i DatabaseHelper objekta.
     */
    override fun onCreate(): Boolean {
        mDbHelper = ZivotinjaDbHelper(context)
        return true
    }

    /**
     * Izvođenje upita za dani URI. Koriste se projekcija, selekcija, njeni argumenti, i poredak sortiranja.
     */
    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?,
                       sortOrder: String?): Cursor? {
        var selection = selection
        var selectionArgs = selectionArgs
        val database = mDbHelper!!.readableDatabase

        val cursor: Cursor

        val match = sUriMatcher.match(uri)
        when (match) {
            ZIVOTINJE ->
                // Za kod ZIVOTINJE, upit nad tablicom se može izvršiti direktno
                // pomoću argumenata. Cursor može sadržavati više redova tablice.
                cursor = database.query(ZivotinjaEntry.TABLE_NAME, projection,
                        selection, selectionArgs, null, null, sortOrder)
            ZIVOTINJA_ID -> {
                selection = ZivotinjaEntry._ID + "=?"
                selectionArgs = arrayOf(ContentUris.parseId(uri).toString())
                cursor = database.query(ZivotinjaEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder)
            }
            else -> throw IllegalArgumentException("Nepoznat URI, ne mogu izvršiti upit! $uri")
        }
        // Postavlja obavještajni URI na Cursor
        // kako bi znali za koji URI sadržaja je Cursor kreiran.
        // Ako se podaci na tom URI-ju(parametar funkcije) promijene, onda znamo da moramo ažurirati Cursor
        // getContentResolver() to znači da će CatalogActivity biti automatski obavještena o promjeni
        cursor.setNotificationUri(context!!.contentResolver, uri)

        // Vraća odgovarajući Cursor
        return cursor
    }

    /**
     * Umetanje novih podataka u PružateljSadržaja s datim ContentValues.
     */
    override fun insert(uri: Uri, contentValues: ContentValues?): Uri? {
        val match = sUriMatcher.match(uri)
        when (match) {
            ZIVOTINJE -> return umetniZivotinju(uri, contentValues!!)
            else -> throw IllegalArgumentException("Umetanje nije podržano za $uri")
        }
    }

    // Umetanje životinje u bazu s datim ContentValues parovima.
    // Vraća novi URI za umetnuti red u bazi.
    private fun umetniZivotinju(uri: Uri, vrijednosti: ContentValues): Uri? {
        // Provjera je li ime unešeno
        val ime = vrijednosti.getAsString(ZivotinjaEntry.COLUMN_LJUBIMAC_IME)
                if (ime == null) {throw IllegalArgumentException("Životinja zahtjeva ime!")}

        val spol = vrijednosti.getAsInteger(ZivotinjaEntry.COLUMN_LJUBIMAC_SPOL)
        if (spol == null || !ZivotinjaEntry.jeValjanSpol(spol)) {
            throw IllegalArgumentException("Životinja zahtjeva valjan spol")
        }

        // Ako je težina unesena, provjeri je li <= 0kg
        val tezina = vrijednosti.getAsInteger(ZivotinjaEntry.COLUMN_LJUBIMAC_TEZINA)
        if (tezina != null && tezina < 0) {
            throw IllegalArgumentException("Životinja zahtjeva valjanu težinu")
        }

        // Pasmina ne mora biti unešena

        // Dohvaćamo bazu za upis podatka
        val database = mDbHelper!!.writableDatabase
        // Umetanje nove životinje s datim vrijednostima
        val id = database.insert(ZivotinjaEntry.TABLE_NAME, null, vrijednosti)

        // Ako je vrijednost ID-a -1, umetanje nije uspjelo. Ispisujemo grešku i vraćamo null.
        if (id < 0) {
            Log.e(LOG_TAG, "Neuspješno umetanje retka za $uri")
            return null
        }
        // Obavijesti sve Listenere (osluškivaće) da se podatak promjenio za URI sadržaja o životinji
        // Npr. Cursor u CatalogActivity osluškuje obavijest za dolje dati URI
        // URI: content://com.example.android.pets/zivotinje
        // Ova linija koda nam je ključna za automatsko učitavanje sadržaja
        context!!.contentResolver.notifyChange(uri, null)

        // Kad saznamo ID novog reda u tablici, možemo proslijediti njegov URI
        return ContentUris.withAppendedId(uri, id)
    }

    /**
     * Ažuriranje podataka na danoj selekciji i njezinim argumentima, s novim ContentValues.
     */
    override fun update(uri: Uri, contentValues: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        var selection = selection
        var selectionArgs = selectionArgs
        val match = sUriMatcher.match(uri)
        when (match) {
            ZIVOTINJE -> return izmjeniZivotinju(uri, contentValues!!, selection, selectionArgs)
            ZIVOTINJA_ID -> {
                // Za ZIVOTINJA_ID kod, izdvoji ID iz URI-ja,
                // kako bi znali koji red izmjeniti. Selekcija će biti "_id=?", a
                // njezini argumenti bit će String koji sadrži pravi ID.
                selection = ZivotinjaEntry._ID + "=?"
                selectionArgs = arrayOf(ContentUris.parseId(uri).toString())
                return izmjeniZivotinju(uri, contentValues!!, selection, selectionArgs)
            }
            else -> throw IllegalArgumentException("Update is not supported for $uri")
        }
    }

    /**
     * Izmjena životinja u bazi davanjem novih vrijednosti. Promjene će se primjeniti na redove
     * zadane selekcijom i njenim argumentima (može se zahvatiti 0, 1 ili više životinja).
     * Vraća broj redova koji su uspješno izmjenjeni.
     */
    private fun izmjeniZivotinju(uri: Uri, values: ContentValues, selection: String?, selectionArgs: Array<String>?): Int {
        // Ukoliko je {@link ZivotinjaEntry#COLUMN_LJUBIMAC_IME} ključ prisutan,
        // provjerava se da vrijednost imena nije null.
        if (values.containsKey(ZivotinjaEntry.COLUMN_LJUBIMAC_IME)) {
            val ime = values.getAsString(ZivotinjaEntry.COLUMN_LJUBIMAC_IME)
                    if (ime == null) {throw IllegalArgumentException("Životinja zahtjeva ime")}
        }

        // Ukoliko je {@link ZivotinjaEntry#COLUMN_LJUBIMAC_SPOL} ključ prisutan,
        // provjerava se je li vrijednost spola valjana.
        if (values.containsKey(ZivotinjaEntry.COLUMN_LJUBIMAC_SPOL)) {
            val spol = values.getAsInteger(ZivotinjaEntry.COLUMN_LJUBIMAC_SPOL)
            if (spol == null || !ZivotinjaEntry.jeValjanSpol(spol)) {
                throw IllegalArgumentException("Životinja zahtjeva valjan spol")
            }
        }

        // Ukoliko je {@link ZivotinjaEntry#COLUMN_LJUBIMAC_TEZINA} ključ prisutan,
        // provjerava se je li vrijednost težine valjana.
        if (values.containsKey(ZivotinjaEntry.COLUMN_LJUBIMAC_TEZINA)) {
            // Provjerava se da li je težina veća ili jednaka 0 kg
            val tezina = values.getAsInteger(ZivotinjaEntry.COLUMN_LJUBIMAC_TEZINA)
            if (tezina != null && tezina < 0) {
                throw IllegalArgumentException("Životinja zahtjeva valjanu težinu")
            }
        }

        // Ne treba provjeravati pasminu, bilo koja vrijednost je prihvatljiva.

        // Ukoliko nema vrijednosti za izmjeniti, ne treba pokušavati izmjeniti bazu
        if (values.size() == 0) {
            return 0
        }

        // Ako je sve u redu, pozovi funkciju getWriteableDatabase() za izmjenu podataka
        val database = mDbHelper!!.writableDatabase

        // Vraća se broj redova baze zahvaćenih izmjenama
        // Izvedi operaciju ažuriranja nad bazom, te dohvati broj zahvaćenih redova
        val redoviUpdated = database.update(ZivotinjaEntry.TABLE_NAME, values, selection, selectionArgs)
        // Ako su 1 ili više redova ažurirani, tada obavjesti sve slušače (listenere) da se
        // podatak na danom URI-ju promjenio
        if (redoviUpdated != 0) {
            context!!.contentResolver.notifyChange(uri, null)
        }
        // Vraća broj ažuriranih redova
        return redoviUpdated
    }

    /**
     * Brisanje podataka za danu selekciju i njezine argumente.
     */
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        var selection = selection
        var selectionArgs = selectionArgs
        // Dohvaćanje baze za "upis"
        val database = mDbHelper!!.writableDatabase

        // Prati broj obrisanih redova
        val obrisaniRedovi: Int

        val match = sUriMatcher.match(uri)
        when (match) {
            ZIVOTINJE ->
                // Obriši sve redove koji odgovaraju selekciji i njezinim argumentima
                obrisaniRedovi = database.delete(ZivotinjaEntry.TABLE_NAME, selection, selectionArgs)
            ZIVOTINJA_ID -> {
                // Brisanje retka zadanog ID-om u URI-u
                selection = ZivotinjaEntry._ID + "=?"
                selectionArgs = arrayOf(ContentUris.parseId(uri).toString())
                obrisaniRedovi = database.delete(ZivotinjaEntry.TABLE_NAME, selection, selectionArgs)
            }
            else -> throw IllegalArgumentException("Brisanje nije podržano za $uri")
        }
        // Ako je 1 ili više redova obrisano, obavjesti sve osluškivaće (Listenere)
        // da se podatak na danom URI-ju promjenio
        if (obrisaniRedovi != 0) {
            context!!.contentResolver.notifyChange(uri, null)
        }
        // Vrati broj obrisanih redova
        return obrisaniRedovi
    }

    /**
     * Returns the MIME type of data for the content URI.
     */
    override fun getType(uri: Uri): String? {
        val match = sUriMatcher.match(uri)
        when (match) {
            ZIVOTINJE -> return ZivotinjaEntry.CONTENT_LIST_TYPE
            ZIVOTINJA_ID -> return ZivotinjaEntry.CONTENT_ITEM_TYPE
            else -> throw IllegalStateException("Nepoznat URI $uri s poklapanjem $match")
        }
    }

    companion object {

        // URI matcher kod za URI sadržaja tablice Zivotinje
        private val ZIVOTINJE = 100

        // URI matcher kod za URI sadržaja pojedine životinje u tablici
        private val ZIVOTINJA_ID = 101

        // URI matcher objekt za uparivanje URI sadržaja odgovarajućem kodu
        // Ulaz proslijeđen u konstruktor prezentira kod koji će vratiti za URI korijen
        // Zadano se koristi NO_MATCH s obzirom da mu još ništa nije dodijeljeno
        private val sUriMatcher = UriMatcher(UriMatcher.NO_MATCH)

        // Postavljanje fiksne vrijednosti.
        init {
            // Tu idu pozivi metode addUri() za sav sadržaj URI uzoraka koje pružatelj
            // (Provider) može prepoznati. Svi putevi dodani UriMatcher-u imaju odgovarajući
            // kod za vratiti kada se pronađe uzorak.

            // URI-ju sadržaja oblika "content://com.example.android.pets/zivotinje" će dodjeliti
            // brojčani kod (ZIVOTINJE). Navedeni URI će se koristiti omogućavanje pristupa više redova tablice.
            sUriMatcher.addURI(ZivotinjaContract.CONTENT_AUTHORITY, ZivotinjaContract.PATH_ZIVOTINJE, ZIVOTINJE)

            // URI-ju sadržaja oblika "content://com.example.android.pets/zivotinje/#" će dodjeliti
            // brojčani kod (ZIVOTINJA_ID). Navedeni URI će se koristiti omogućavanje pristupa jednom redu tablice.
            // U ovom slučaju # se koristi kao zamjena za broj retka u bazi.
            sUriMatcher.addURI(ZivotinjaContract.CONTENT_AUTHORITY, ZivotinjaContract.PATH_ZIVOTINJE + "/#", ZIVOTINJA_ID)
        }

        /** Oznaka za poruke sustava  */
        val LOG_TAG = ZivotinjaProvider::class.java.simpleName
    }
}