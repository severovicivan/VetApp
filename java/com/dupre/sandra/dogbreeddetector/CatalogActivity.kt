package com.dupre.sandra.dogbreeddetector

import android.app.LoaderManager
import android.content.*
import android.database.Cursor
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.ListView
import android.widget.RelativeLayout

import com.dupre.sandra.dogbreeddetector.podaci.ZivotinjaContract.ZivotinjaEntry

/**
 * Prikaz liste životinja koje su unesene i pohranjene u aplikaciju.
 */
class CatalogActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<Cursor> {

    internal lateinit var mCursorAdapter: ZivotinjaCursorAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.glavni_izbornik)

        // Podešavanje FAB za otvaranje EditorActivity
        val fab = findViewById(R.id.fab) as FloatingActionButton
        fab.setOnClickListener {
            val intent = Intent(this@CatalogActivity, EditorActivity::class.java)
            startActivity(intent)
        }

        // Tražimo ListView koji ćemo popuniti s podacima o životinjama
        val zivotinjaListView = findViewById(R.id.lista) as ListView

        // Pronađi i postavi prazni pogled na ListView, tako da se on prikazuje samo kad lista ima 0 stavki
        val emptyView = findViewById<RelativeLayout>(R.id.prazni_pogled)
        zivotinjaListView.emptyView = emptyView

        // Postavi Adapter da kreira stavku liste za svaki red podataka o životinji u Cursoru
        // Podataka još uvijek nema sve dok se učitavanje ne završi (onLoadFinished) tako
        // da Cursoru prosljeđuje null
        mCursorAdapter = ZivotinjaCursorAdapter(this, null)
        zivotinjaListView.adapter = mCursorAdapter

        // Postavljamo na listu osluškivać dodira stavke
        zivotinjaListView.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, pozicija, ID ->
            // pozicija = pozicija stavke unutar liste
            // ID = id stavke
            // Kreiramo novi Intent da bi otišli u EditorActivity
            val intent = Intent(this@CatalogActivity, EditorActivity::class.java)

            // Formiranje URI-ja sadržaja koji predstavlja pojedinu životinju koju smo odabrali,
            // dodavanjem id-a (ulazni parametar metode) na CONTENT_URI
            // Npr. URI: "content://com.example.android/pets/zivotinje/2"
            // Ako smo kliknuli na životinju s ID-om 2
            val trenutnaZivotinjaUri = ContentUris.withAppendedId(ZivotinjaEntry.CONTENT_URI, ID)

            // Postavljamo identifikator resursa na podatkovno polje Intenta
            intent.data = trenutnaZivotinjaUri

            // Pokrećemo EditorActivity za prikaz podataka o trenutnoj životinji
            startActivity(intent)
        }

        var b: String? = "abc"
        // Pokreni loader
        loaderManager.initLoader(ZIVOTINJA_LOADER, Bundle.EMPTY , this)
    }

    // Pomoćna metoda za umetanje nasumičnih podataka o životinji u bazu.
    private fun umetniZivotinju() {
        // Stvara ContentValues objekt u kojem su stupci ključevi,
        // a Rexovi atributi vrijednosti
        val vrijednosti = ContentValues()
        vrijednosti.put(ZivotinjaEntry.COLUMN_LJUBIMAC_IME, "Rex")
        vrijednosti.put(ZivotinjaEntry.COLUMN_LJUBIMAC_PASMINA, "Njemacki ovcar")
        vrijednosti.put(ZivotinjaEntry.COLUMN_LJUBIMAC_SPOL, ZivotinjaEntry.SPOL_MUSKO)
        vrijednosti.put(ZivotinjaEntry.COLUMN_LJUBIMAC_TEZINA, 20)

        // Umetanje novog retka za Rexa u Provider korištenjem ContentResolvera.
        // Koristi se CONTENT_URI iz tablice Zivotinje da bi dali do znanja
        // kako želimo obaviti umetanje u tablicu Zivotinje.
        // Prima se novi URI koji će nam omogućiti pristup Rexovim podacima u budućnosti.
        val noviUri = contentResolver.insert(ZivotinjaEntry.CONTENT_URI, vrijednosti)
    }

    /**
     * Pomoćna metoda za brisanje svih životinja iz baze.
     */
    private fun obrisiSveZivotinje() {
        val rowsDeleted = contentResolver.delete(ZivotinjaEntry.CONTENT_URI, null, null)
        Log.v("CatalogActivity", rowsDeleted.toString() + " rows deleted from pet database")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        menuInflater.inflate(R.menu.menu_catalog, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Korisnik je odabrao opciju izbornika
        when (item.itemId) {
            // Odgovor na odabir opcije "Umetni izmišljene podatke"
            R.id.action_umetni_lazne_podatke -> {
                umetniZivotinju()
                return true
            }
            // Odgovor na odabir opcije "Obriši sve"
            R.id.action_obrisi_sve_unose -> {
                obrisiSveZivotinje()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateLoader(i: Int, bundle: Bundle): Loader<Cursor> {
        // Definiramo projekciju koja određuje stupce iz tablice koji su nam potrebni
        val projekcija = arrayOf<String>(ZivotinjaEntry._ID, ZivotinjaEntry.COLUMN_LJUBIMAC_IME, ZivotinjaEntry.COLUMN_LJUBIMAC_PASMINA)

        // Loader će potom izvršiti Content Providerov upit na pozadinskoj niti
        return CursorLoader(this, // Context roditeljske aktivnosti
                ZivotinjaEntry.CONTENT_URI, // Dostavljač URI sadržaja upitu
                projekcija, null, null, null)// Stupci koje će uključiti rezultirajući Cursor
        // Nema selekcije
        // Nema argumenata selekcije
        // Sortiranje prema zadanim postavkama
    }

    override fun onLoadFinished(loader: Loader<Cursor>, podaci: Cursor) {
        // Ažuriranje ZivotinjaCursorAdaptera novim Cursorom koji sadrži ažurirane podatke
        mCursorAdapter.swapCursor(podaci)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        // Callback(uzvraćanje poziva) pozvano kada podatke treba obrisati
        mCursorAdapter.swapCursor(null)
    }

    companion object {

        private val ZIVOTINJA_LOADER = 0
    }
}