package com.dupre.sandra.dogbreeddetector

import android.content.Context
import android.database.Cursor
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.TextView

import com.dupre.sandra.dogbreeddetector.podaci.ZivotinjaContract

/**
 * [ZivotinjaCursorAdapter] je adapter za listu ili rešetkasti pogled (View)
 * koji koristi [Cursor] podataka o životinjama kao izvor podataka. Adapter zna
 * kako kreirati listu stavki za svaki red životinjskih podataka iz [Cursor].
 */
class ZivotinjaCursorAdapter
/**
 * Konstruira novi [ZivotinjaCursorAdapter].
 *
 * @param context Kontekst
 * @param c       Cursor iz kojeg uzima podatke.
 */
(context: Context, c: Cursor?)/* flags */ : CursorAdapter(context, c, 0) {

    /**
     * Izrada novog praznog pogleda liste stavki. Još nema nikakvih podataka vezanih na njega.
     *
     * @param context kontekst u aplikaciji
     * @param cursor  Cursor s kojeg dohvaća podatke. Cursor je već postavljen na traženu poziciju
     * @param parent  Roditelj Za kojeg je novi pogled prikvačen
     * @return novo stvoreni pogled stavke liste.
     */
    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        return LayoutInflater.from(context).inflate(R.layout.stavka_liste, parent, false)
    }

    /**
     * Ova metoda veže podatke životinje (trenutnog reda na koji Cursor pokazuje) za dani
     * pogled stavke liste. Npr. ime za trenutnu životinju može se postaviti na TextView id=ime
     * u pogledu stavka_liste.xml.
     *
     * @param view    Postojeći pogled, vraćen prethodno od newView() metode
     * @param context Kontekst u aplikaciji
     * @param cursor  Cursor s kojeg se dohvaćaju podaci. Već je postavljen na traženi red.
     */
    override fun bindView(view: View, context: Context, cursor: Cursor) {
        // Tražimo poglede u stavka_liste.xml koje želimo izmjeniti
        val imeTextView = view.findViewById(R.id.ime) as TextView
        val pojedinostiTextView = view.findViewById(R.id.pregled) as TextView

        // Tražimo stupce koji su nam interesantni za prikaz
        val imeStupacIndex = cursor.getColumnIndex(ZivotinjaContract.ZivotinjaEntry.COLUMN_LJUBIMAC_IME)
        val pasminaStupacIndex = cursor.getColumnIndex(ZivotinjaContract.ZivotinjaEntry.COLUMN_LJUBIMAC_PASMINA)

        // Čitamo vrijednosti s kursora za trenutnu životinju
        val zivotinjaIme = cursor.getString(imeStupacIndex)
        var zivotinjaPasmina = cursor.getString(pasminaStupacIndex)

        // Ukoliko je pasmina prazan string ili null, koristi predloženi tekst
        // koji govori "Nepoznata pasmina", tako da TextView nije prazan.
        if (TextUtils.isEmpty(zivotinjaPasmina)) {
            zivotinjaPasmina = context.getString(R.string.nepoznata_pasmina)
        }

        // Ažuriramo TextView s iščitanim podacima
        imeTextView.text = zivotinjaIme
        pojedinostiTextView.text = zivotinjaPasmina


    }
}