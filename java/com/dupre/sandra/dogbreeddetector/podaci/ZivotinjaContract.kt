package com.dupre.sandra.dogbreeddetector.podaci

import android.content.ContentResolver
import android.net.Uri
import android.provider.BaseColumns

object ZivotinjaContract {

    /**
     * "Content authority" je ime za content provider (ZivotinjaProvider), slično
     * vezi između domene i njezine web stranice.  Dogovoren string za
     * content authority je ime paketa aplikacije, što garantira jedinstvenost
     * na uređaju.
     */
    val CONTENT_AUTHORITY = "com.dupre.sandra.dogbreeddetector"

    /**
     * Koristimo CONTENT_AUTHORITY za kreiranje baze svih URI-ja koje će aplikacija koristiti
     * za kontaktiranje content provider. Da bi se URI moglo koristiti, koristimo parse()
     * koja prima URI string a vraća Uri objekt.
     */
    val BASE_CONTENT_URI = Uri.parse("content://$CONTENT_AUTHORITY")

    /**
     * Moguća putanja (dodatak na BASE_CONTENT_URI za moguće URI-je)
     * Npr. content://com.example.android.pets/zivotinje/ je važeća putanja za
     * traženje podataka o životinji. content://com.example.android.pets/veterinari/ neće proći,
     * s obzirom da ContentProvider nije dobio nikakvu informaciju što raditi s "veterinari".
     */
    val PATH_ZIVOTINJE = "zivotinje"

    /**
     * Unutarnja klasa koja definira veijednosti tablice baze.
     * Svaki zapis u tablici predstavlja pojedinog ljubimca.
     */
    class ZivotinjaEntry : BaseColumns {
        companion object {

            /** URI koji služi za pristup sadržajima unutar pružatelja (Providera)  */
            val CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_ZIVOTINJE)

            /**
             * MIME tip [.CONTENT_URI] za listu životinja.
             * MIME je oznaka koja se koristi da bi identificirali tip podatka.
             * npr. ako server kaže application/pdf klijent zna da traženi podatak može otvoriti u pdf čitaču.
             */
            val CONTENT_LIST_TYPE =
                    ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_ZIVOTINJE

            /**
             * MIME tip [.CONTENT_URI] za pojedinu životinju.
             */
            val CONTENT_ITEM_TYPE =
                    ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_ZIVOTINJE


            val TABLE_NAME = "zivotinje"

            val _ID = BaseColumns._ID
            val COLUMN_LJUBIMAC_IME = "ime"
            val COLUMN_LJUBIMAC_PASMINA = "pasmina"
            val COLUMN_LJUBIMAC_SPOL = "spol"
            val COLUMN_LJUBIMAC_TEZINA = "tezina"

            val SPOL_NEPOZNAT = 0
            val SPOL_MUSKO = 1
            val SPOL_ZENSKO = 2

            /**
             * Vraća jesu li [.SPOL_NEPOZNAT], [.SPOL_MUSKO],
             * [.SPOL_ZENSKO] jedna od unešenih vrijednosti.
             */
            fun jeValjanSpol(spol: Int): Boolean {
                return if (spol == SPOL_NEPOZNAT || spol == SPOL_MUSKO || spol == SPOL_ZENSKO) {
                    true
                } else false
            }
        }
    }
}// Da bi spriječili nekoga od slučajnog instanciranja Contract klase,
// dat ćemo mu prazan konstruktor.