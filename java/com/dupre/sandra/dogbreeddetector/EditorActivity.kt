package com.dupre.sandra.dogbreeddetector

import android.app.AlertDialog
import android.app.LoaderManager
import android.content.CursorLoader
import android.content.DialogInterface
import android.content.ContentValues
import android.content.Intent
import android.content.Loader
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.NavUtils
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*

import com.dupre.sandra.dogbreeddetector.podaci.ZivotinjaContract.ZivotinjaEntry

/**
 * Dozvoljava korisniku stvaranje ili uređivanje postojeće životinje.
 */
class EditorActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * URI za postojeću životinju (null ukoliko je nova životinja)
     */
    private var mTrenutnaZivotinjaUri: Uri? = null

    /**
     * EditText polje za unos imena životinje
     */
    private var mImeEditText: EditText? = null

    /**
     * EditText za unos pasmine
     */
    private var mPasminaEditText: EditText? = null

    /**
     * EditText polje za unos težine
     */
    private var mTezinaEditText: EditText? = null

    /**
     * EditText polje za unos spola
     */
    private var mSpolSpinner: Spinner? = null

    /**
     * Spol životinje. Moguće vrijednosti:
     * 0 za nepoznat spol, 1 za mužjaka, 2 za ženku.
     */
    private var mSpol = ZivotinjaEntry.SPOL_NEPOZNAT
    /**
     * Boolean flag that keeps track of whether the pet has been edited (true) or not (false)
     */
    private var mZivotinjaSePromjenila = false
    /**
     * OnTouchListener osluškuje korisnikove dodire na View, odnosno na izmjenu
     * pogleda, tad mijenjamo mPetHasChanged boolean na true.
     */
    private val mDodirListener = View.OnTouchListener { view, motionEvent ->
        mZivotinjaSePromjenila = true
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.unos_novog)

        // Podešavanje guba za otvaranje SlikanjeActivity
        val fab = findViewById(R.id.button_id) as Button
        fab.setOnClickListener {
            val intent = Intent(this@EditorActivity, PrepoznajZivotinju::class.java)
            startActivity(intent)
        }

        // Pregledavamo intent koji je iskorišten za pokretanje ove aktivnosti,
        // kako bismo shvatili kreiramo li novu životinju ili mijenjamo postojeću
        val intent = intent
        mTrenutnaZivotinjaUri = intent.data

        // Ukoliko intent NE SADRŽI uri životinje,
        // tada znamo da kreiramo novu životinju
        if (mTrenutnaZivotinjaUri == null) {
            // Ovo je nova životinja, mijenjamo naslov trake u "Dodaj životinju"
            title = getString(R.string.unos_novog_naslov_nova_zivotinja)
            // Onemogući opcije, tako da "Obriži" opcija izbornika može biti skrivena.
            // (Nema smisla obrisati životinju koja još nije niti kreirana.)
            invalidateOptionsMenu()
        } else {
            // U suprotnom, to je postojeća životinja, pa mijenjamo naslov trake
            title = getString(R.string.unos_novog_naslov_uredi_zivotinju)

            // Inicijalizacija loadera za čitanje sadržaja o životinjama iz baze
            // te prikaz trenutne vrijednosti u editoru
            loaderManager.initLoader(POSTOJECI_ZIVOTINJA_LOADER, null, this)
        }

        // Tražimo sve poglede koji će nam biti potrebni za čitanje korisničkog unosa iz
        mImeEditText = findViewById(R.id.edit_pet_name) as EditText
        mPasminaEditText = findViewById(R.id.edit_pet_breed) as EditText
        mTezinaEditText = findViewById(R.id.edit_pet_weight) as EditText
        mSpolSpinner = findViewById(R.id.spinner_gender) as Spinner

        // Postavljanje OnTouchListenera na sva polja unosa, kako bi odredili ukoliko ih je korisnik
        // dotaknuo ili izmjenio. To će nam dati do znanja postoje li nespremljene promjene
        // ili ne, ako korisnik pokuša napustiti editor bez spremanja.
        mImeEditText!!.setOnTouchListener(mDodirListener)
        mPasminaEditText!!.setOnTouchListener(mDodirListener)
        mTezinaEditText!!.setOnTouchListener(mDodirListener)
        mSpolSpinner!!.setOnTouchListener(mDodirListener)

        setupSpinner()
    }

    /**
     * Postavljanje padajućeg izbornika koji omogućava korisniku odabir spola.
     */
    private fun setupSpinner() {
        // Kreiranje adaptera za spinner. The list options are from the String array it will use
        // the spinner will use the default layout
        val spolSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_gender_options, android.R.layout.simple_spinner_item)

        // Određivanje stila padajućeg izbornika - ListView s jednom stavkom po liniji
        spolSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)

        // Primjena adaptera na spinner
        mSpolSpinner!!.adapter = spolSpinnerAdapter

        // Set the integer mSelected to the constant values
        mSpolSpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selection = parent.getItemAtPosition(position) as String
                if (!TextUtils.isEmpty(selection)) {
                    if (selection == getString(R.string.spol_musko)) {
                        mSpol = ZivotinjaEntry.SPOL_MUSKO // Mužjak
                    } else if (selection == getString(R.string.spol_zensko)) {
                        mSpol = ZivotinjaEntry.SPOL_ZENSKO // Ženka
                    } else {
                        mSpol = ZivotinjaEntry.SPOL_NEPOZNAT // Nepoznato
                    }
                }
            }

            // S obzirom da je AdapterView apstraktna klasa, moramo definirati onNothingSelected
            override fun onNothingSelected(parent: AdapterView<*>) {
                mSpol = 0 // Nepoznat spol
            }
        }
    }

    // Dohvaćanje unosa korisnika i njegovo spremanje u bazu
    private fun spremiZivotinju() {
        // Čitanje s polja za unos
        // Korištenje trim metode eliminiramo vodeći ili prateći razmak
        val imeString = mImeEditText!!.text.toString().trim { it <= ' ' }
        val pasminaString = mPasminaEditText!!.text.toString().trim { it <= ' ' }
        val tezinaString = mTezinaEditText!!.text.toString().trim { it <= ' ' }
        // Provjera treba li ovo biti novi ljubimac
        // i provjera jesu li sva polja u editoru prazna
        if (mTrenutnaZivotinjaUri == null && TextUtils.isEmpty(imeString)
                && TextUtils.isEmpty(pasminaString) && TextUtils.isEmpty(tezinaString)
                && mSpol == ZivotinjaEntry.SPOL_NEPOZNAT) {
            // S obzirom da nema izmjenjenih polja, možemo se vratiti bez kreiranja nove životinje.
            // Nema potrebe za kreiranjem ContentValues i rađenjem bilo kakvih ContentProvider operacija.
            return
        }


        // Kreiramo ContentValues objekt gdje su nazivi stupaca ključevi,
        // a unešeni atributi vrijednosti
        val vrijednosti = ContentValues()
        vrijednosti.put(ZivotinjaEntry.COLUMN_LJUBIMAC_IME, imeString)
        vrijednosti.put(ZivotinjaEntry.COLUMN_LJUBIMAC_PASMINA, pasminaString)
        vrijednosti.put(ZivotinjaEntry.COLUMN_LJUBIMAC_SPOL, mSpol)
        // Ako korisnik nije unio težinu, ne pokušavaj pretvarati string u broj
        // Koristi 0 kao zadano
        var tezina = 0
        if (!TextUtils.isEmpty(tezinaString)) {
            tezina = Integer.parseInt(tezinaString)
        }
        vrijednosti.put(ZivotinjaEntry.COLUMN_LJUBIMAC_TEZINA, tezina)

        // Odredi je li ovo nova ili postojeća životinja provjerom ako je mTrenutnaZivotinjaUri null ili nije
        if (mTrenutnaZivotinjaUri == null) {
            // Umetanje novog reda u bazu  i vraćanje njegovog URI-a.
            // Prvi argument za insert() metodu je putanja tablice.
            // Drugi argument je ContentValues objekt koji sadrži informacije o NOVOJ životinji.
            val noviUri = contentResolver.insert(ZivotinjaEntry.CONTENT_URI, vrijednosti)

            // Prikaz Toast poruke o uspješnosti umetanja.
            if (noviUri == null) {
                // Ukoliko je URI novog sadržaja null, pojavila se greška prilikom umetanja.
                Toast.makeText(this, getString(R.string.editor_unos_zivotinje_neuspjesno),
                        Toast.LENGTH_SHORT).show()
            } else {
                // Ili je umetanje bilo uspješno, te se prikazuje poruka
                Toast.makeText(this, getString(R.string.editor_unos_zivotinje_uspjesno), Toast.LENGTH_SHORT).show()
            }
        } else {
            // Inače, radi se o POSTOJEĆOJ životinji, tako da se ažurira životinja s URI: mTrenutnaZivotinjaUri
            // te se prosljeđuje novi ContentValues. Prosljeđuje se null za selekciju i njene argumente
            // jer mTrenutnaZivotinjaUri će već identificirati točan red u bazi koji želimo izmjeniti
            val zahvaceniRedovi = contentResolver.update(mTrenutnaZivotinjaUri!!, vrijednosti, null, null)
            // Prikaz Toast poruke o uspješnosti umetanja.
            if (zahvaceniRedovi == 0) {
                // Ako nije bilo zahvaćenih redova, pojavila se greška tokom ažuriranja.
                Toast.makeText(this, getString(R.string.editor_azuriranje_zivotinje_neuspjesno),
                        Toast.LENGTH_SHORT).show()
            } else {
                // Ili je umetanje bilo uspješno, te se prikazuje poruka
                Toast.makeText(this, getString(R.string.editor_azuriranje_zivotinje_uspjesno),
                        Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        menuInflater.inflate(R.menu.menu_editor, menu)
        return true
    }

    /**
     * Ova metoda se poziva nakon invalidateOptionsMenu(), tako da
     * menu može biti ažuriran (neke stavke menija mogu biti skrivene ili biti napravljene vidljivima).
     */
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        // Ako je ovo nova životinja, sakrij "Obriši" stavku menija.
        if (mTrenutnaZivotinjaUri == null) {
            val menuItem = menu.findItem(R.id.action_delete)
            menuItem.isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Korisnik je kliknuo na opcije
        when (item.itemId) {
            // Odgovor na odabir opcije "Spremi"
            R.id.action_save -> {
                // Spremi životinju u bazu
                spremiZivotinju()
                // Izlazak iz aktivnosi
                finish()
                return true
            }
            // Odgovor na odabir opcije "Obriši"
            R.id.action_delete ->
                // Do nothing for now
                return true
            // Odgovor na strelicu "Gore" u statusnoj traci
            android.R.id.home -> {
                // ako se životinja nije izmjenila, nastavi s navigacijom na roditeljsku aktivnost
                // koja je {@link CatalogActivity}.
                if (!mZivotinjaSePromjenila) {
                    NavUtils.navigateUpFromSameTask(this@EditorActivity)
                    return true
                }
                // Inače ako postoje nespremljene promjene, postavi dijalog koji će upozoriti korisnika.
                // Kreirak klik listener za rukovanje s korisnikovom potvrdom da
                // bi promjene trebale biti poništene.
                val discardButtonClickListener = DialogInterface.OnClickListener { dialogInterface, i ->
                    // Korisnik je odabrao "Poništi" gumb vrati se na roditeljsku aktivnost.
                    NavUtils.navigateUpFromSameTask(this@EditorActivity)
                }
                // Prikaži dijalog koji obavještava korisnika da ima nespremljenih promjena
                showUnsavedChangesDialog(discardButtonClickListener)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * This method is called when the back button is pressed.
     */
    override fun onBackPressed() {
        // Ukoliko se životinja nije promjenila, nastavi s rukovanjem dodira za nazad
        if (!mZivotinjaSePromjenila) {
            super.onBackPressed()
            return
        }
        // Inače ako postoje nespremljene promjene, postavi dijalog upozorenja korisniku.
        // Kreiraj osluškivač dodira za rukovanje korisnikovog potvrđivanja da bi promjene trebale biti poništene.
        val discardButtonClickListener = DialogInterface.OnClickListener { dialogInterface, i ->
            // Korisnik je odabrao "Poništi" dugme, zatvori trenutnu aktivnost.
            finish()
        }
        // Prikaži dijalog da postoje nespremljene promjene
        showUnsavedChangesDialog(discardButtonClickListener)
    }

    override fun onCreateLoader(i: Int, bundle: Bundle): Loader<Cursor> {
        // S obzirom da editor prikazuje sve atribute životinje, definiramo projekciju
        // koja sadrži sve stupce tablice Zivotinje
        val projection = arrayOf<String>(ZivotinjaEntry._ID, ZivotinjaEntry.COLUMN_LJUBIMAC_IME, ZivotinjaEntry.COLUMN_LJUBIMAC_PASMINA, ZivotinjaEntry.COLUMN_LJUBIMAC_SPOL, ZivotinjaEntry.COLUMN_LJUBIMAC_TEZINA)

        // Ovaj Loader će izvršiti ContentProvider-ovu query metodu na pozadinskoj niti
        return CursorLoader(this, // Kontekst roditeljske aktivnosti
                mTrenutnaZivotinjaUri, // Tražimo URI za trenutnu životinju
                projection, null, null, null)// Stupci koji će biti u Cursoru
        // Nema selekcije
        // Niti argumenata selekcije
        // Zadani poredak sortiranja
    }

    override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor?) {
        // Izađi ako je Cursor null ili ima u sebi manje od jednog reda
        if (cursor == null || cursor.count < 1) {
            return
        }
        // Nastavi s pozicioniranjem do prvog reda Cursora i pročitaj podatke s njega
        // (To bi ujedno trebao biti i jedini red u Cursoru)
        if (cursor.moveToFirst()) {
            // Pronađi stupce atributa životinje koji nas zanimaju
            val imeStupacIndex = cursor.getColumnIndex(ZivotinjaEntry.COLUMN_LJUBIMAC_IME)
            val pasminaStupacIndex = cursor.getColumnIndex(ZivotinjaEntry.COLUMN_LJUBIMAC_PASMINA)
            val spolStupacIndex = cursor.getColumnIndex(ZivotinjaEntry.COLUMN_LJUBIMAC_SPOL)
            val tezinaStupacIndex = cursor.getColumnIndex(ZivotinjaEntry.COLUMN_LJUBIMAC_TEZINA)
            // Izdvoji vrijednosti iz Cursora za date indexe stupca
            val ime = cursor.getString(imeStupacIndex)
            val pasmina = cursor.getString(pasminaStupacIndex)
            val spol = cursor.getInt(spolStupacIndex)
            val tezina = cursor.getInt(tezinaStupacIndex)
            // Update the views on the screen with the values from the database
            mImeEditText!!.setText(ime)
            mPasminaEditText!!.setText(pasmina)
            mTezinaEditText!!.setText(Integer.toString(tezina))
            // Spol je padajući spinner, znači da mapira konstantnu vrijednost iz baze
            // u jednu od padajućih opcija (0 je Nepoznat, 1 je Muško, 2 je Žensko).
            // Zatim poziva setSelection() kako bi se ta opcija prikazala na zaslonu za dati odabir.
            when (spol) {
                ZivotinjaEntry.SPOL_MUSKO -> mSpolSpinner!!.setSelection(1)
                ZivotinjaEntry.SPOL_ZENSKO -> mSpolSpinner!!.setSelection(2)
                else -> mSpolSpinner!!.setSelection(0)
            }
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        // Ako je loader poništen, očisti sve podatke s polja za unos.
        mImeEditText!!.setText("")
        mPasminaEditText!!.setText("")
        mTezinaEditText!!.setText("")
        mSpolSpinner!!.setSelection(0) // Select "Unknown" gender
    }

    /**
     * Prikaži dijalog koji upozorava korisnika o nespremljenim promjenama koje će biti izgubljene
     * ukoliko nastavi s napuštanjem editora.
     *
     * @param discardButtonClickListener je osluškivač dodira za odluku što raditi kada
     * korisnik potvrdi da želi poništiti promjene
     */
    private fun showUnsavedChangesDialog(
            discardButtonClickListener: DialogInterface.OnClickListener) {
        // Kreiraj AlertDialog.Builder i postavi poruku, te osluškivač dodira
        // za pozitivne i negativne gumbe na dijalogu.
        val builder = AlertDialog.Builder(this)
        builder.setMessage(R.string.nespremljene_promjene_dialog_msg)
        builder.setPositiveButton(R.string.ponisti, discardButtonClickListener)
        builder.setNegativeButton(R.string.nastavi_uredivati) { dialog, id ->
            // Korisnik je kliknuo "Nastavi uređivati" gumb, pa otpusti dijalog
            // te nastavi s uređivanjem životinje.
            dialog?.dismiss()
        }
        // Kreiraj i pokaži AlertDialog
        val alertDialog = builder.create()
        alertDialog.show()
    }

    companion object {

        /**
         * Identifikator za Loader podataka o žuvotinjama
         */
        private val POSTOJECI_ZIVOTINJA_LOADER = 0
    }
}