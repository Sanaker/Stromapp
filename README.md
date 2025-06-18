⚡️ StrømApp

Velkommen til StrømApp – en enkel, men effektiv Android-applikasjon for å vise nåværende strømpriser og se prisutviklingen for de neste timene i Norge. Appen kobler seg som standard til en ekstern API for å hente data, inkludert MVA- og strømstøtteberegning.

For de som ønsker mer kontroll, er det også inkludert en valgfri Python Flask Backend som lar deg kjøre din egen prishenting og databehandling lokalt eller på din egen server.
🚀 Komme i gang

Følg disse trinnene for å sette opp og kjøre StrømApp lokalt.
Forutsetninger

Før du starter, sørg for at du har følgende installert:

    Android Studio
    Git (for å klone repositoryet)
    Python 3.x (kun hvis du planlegger å kjøre den valgfrie Python Backend)

1. Klone Repositoryet

Start med å klone prosjektet fra GitHub:
Bash

git clone https://github.com/Sanaker/Stromapp.git
cd Stromapp

2. Sette opp Android Appen

Android-appen viser dataene den henter fra en API-kilde.
a. Åpne prosjektet i Android Studio

    Åpne Android Studio.
    Velg "Open an existing Android Studio project" eller "Open".
    Naviger til rotmappen av ditt klonede repository (Stromapp/).
    Android Studio vil nå importere og bygge prosjektet. La det fullføre Gradle-synkroniseringen.

b. Konfigurer API-URLen

Du må fortelle Android-appen hvilken API den skal koble seg til.

    Åpne filen MainActivity.kt (eller tilsvarende Kotlin/Java-fil der du gjør API-kallene dine).

    Finn variabelen som holder URL-en til API-en din. Endre denne til URL-en for din foretrukne strømpris-API.
    Kotlin

    // Eksempel i MainActivity.kt
    private val BASE_URL = "DIN_API_URL_HER" // Sett inn den faktiske URL-en

        Viktig: Pass på at URL-en er korrekt og tilgjengelig fra enheten eller emulatoren du tester på. Hvis du velger å kjøre den valgfrie Python Backend lokalt, bruk http://10.0.2.2:5000/ for emulatorer eller din datamaskins lokale IP-adresse for fysiske enheter (f.eks. http://192.168.1.XXX:5000/).

c. Kjør Android Appen

    Koble til en Android-enhet eller start en emulator i Android Studio.
    Klikk på "Run App" (grønn play-knapp) i verktøylinjen i Android Studio.

Appen skal nå starte og vise strømprisene hentet fra din konfigurerte API.
3. Valgfri: Sette opp Python Flask Backend

For de som ønsker å hoste sin egen strømpris-API, er en Python Flask-backend inkludert. Denne backend'en henter realtids strømpriser fra Nord Pool via nordpool-api-biblioteket, beregner priser med MVA og strømstøtte, og serverer dem.
a. Naviger til Backend-mappen
Bash

cd stromapp/backend # Antar at app.py ligger i en 'backend'-mappe inne i 'stromapp'

b. Opprett et virtuelt miljø (Anbefalt)

Det er god praksis å bruke et virtuelt miljø for å isolere prosjektets avhengigheter:
Bash

python -m venv venv
# På Windows:
.\venv\Scripts\activate
# På macOS/Linux:
source venv/bin/activate

c. Installer avhengigheter

Installer alle nødvendige Python-pakker:
Bash

pip install -r requirements.txt

    Merk: Sørg for at requirements.txt inneholder flask, requests, nordpool-api, pytz, og matplotlib.

d. Konfigurasjon (i app.py)

Du kan tilpasse AREA, MVA_RATE, STROEMSTOTTE_DEKNING, STROEMSTOTTE_GRENSE, og TIMEZONE direkte i app.py for å matche ditt område eller dine foretrukne beregninger.
e. Kjør Flask Backend

Start Flask-serveren. Den vil kjøre på http://0.0.0.0:5000 som standard.
Bash

python app.py

Hvis du kjører denne lokalt, husk å konfigurere BASE_URL i Android-appen din til å peke til denne serveren (se 2b. Konfigurer API-URLen ovenfor).
🛠 Prosjektstruktur
```bash
Stromapp/
├── backend/                  # Valgfri Python Flask backend
│   └── app.py                # Hoved-Flask-applikasjonen
│   └── requirements.txt      # Python-avhengigheter
├── app/                      # Android Studio prosjekt (din Android app)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/yourcompany/stromapp/  # Din Kotlin/Java kode
│   │   │   │       └── MainActivity.kt
│   │   │   ├── res/          # Android ressurser (layouts, farger, strings)
│   │   │   │   ├── layout/
│   │   │   │   │   └── activity_main.xml  # Din UI layout
│   │   │   │   ├── values/
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   └── strings.xml
│   │   │   │   │   └── styles.xml  # Eller themes.xml
│   │   │   │   │   └── dimens.xml
│   ├── build.gradle
├── .gitignore                # Filer og mapper som ignoreres av Git
└── README.md                 # Denne filen
```
✨ Funksjoner

    Henter sanntids strømpriser fra en ekstern API (konfigurerbar).
    Valgfri Python Flask Backend for egen hosting og databehandling (inkluderer MVA- og strømstøtteberegning).
    Viser den nåværende timeprisen tydelig.
    Presenterer en interaktiv graf over prisutviklingen for de neste timene.
    Støtter "pull-to-refresh" i Android-appen for å oppdatere priser.

🎨 Design og Tilpasning

Android-appen bruker XML-layout for brukergrensesnittet. Du kan tilpasse designet ved å endre filene i app/src/main/res/.

    colors.xml: Definer fargeskjemaet ditt (bakgrunner, tekstfarger, aksentfarger).
    styles.xml / themes.xml: Sett globale stiler for tekst, knapper og andre UI-komponenter.
    dimens.xml: Administrer konsistente avstander og størrelser (padding, marginer, tekststørrelser).
    activity_main.xml: Juster layouten, plassering av elementer og bruk av stiler.

Du kan også implementere logikk i MainActivity.kt for å dynamisk endre farger basert på prisnivåer (f.eks. grønn for lave priser, rød for høye priser) for å forbedre brukeropplevelsen.
```
📓 To-Do liste
    1. Legge til instillingsmeny
        1. Endre region
        2. Endre API
        3. Mørk Modus
    2. Mer funksjonalitet for grafen
        1. Ha pris og tidspunkt på punktene i grafen
        2. fjerne zoomingen i grafen som oppstår hvis man dobbeltapper på grafen
        3. kunne ha instillinger som endrer grafen
    3. Auto-Update instilling
        1. mulighet til å legge en auto-update så en kan slippe å dra ned hver gang for å oppdatere prisen
        2. Always On skjerm - legge til en instilling så skjermen aldri skrur seg av (funker bra hvis du har en skjerm bare stående med appen åpen)
    4. (kanskje) mulig med en side nr.2 hvor man får en liste over alle prisene for dagen
    5. Få appen ut på Google-Play Store (Her driver jeg å jobber med saken, bare må få verifisert kontoen min for å laste opp appen)
```
⚠️ Viktig Merknad om Strømprisdata

Hvis du bruker den valgfrie Python-backend, er den avhengig av nordpool-api-biblioteket. Vær oppmerksom på Nord Pools bruksvilkår og eventuelle begrensninger for datatilgang. Dataene som presenteres er for informasjonsformål og bør ikke brukes som eneste grunnlag for økonomiske beslutninger.
🤝 Bidrag

Foreslår du forbedringer eller rapporterer feil? Bidrag er velkomne!
📄 Lisens

Dette prosjektet er lisensiert under GPL-3-0. Se LICENSE-filen for detaljer.
