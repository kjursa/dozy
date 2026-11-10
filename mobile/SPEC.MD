# Dozy — specyfikacja aplikacji mobilnej

## 1. Czym jest aplikacja

Dozy tworzy personalizowane bajki na dobranoc. Aplikację obsługuje przede wszystkim rodzic: definiuje postacie, przygotowuje przygodę i czyta gotową historię dziecku. Bohaterami mogą być dzieci rodzica, zwierzęta albo wymyślone postacie. Miejsca pomagają osadzić opowieść w znajomym lub fantastycznym świecie.

Użytkownik może podać własny pomysł albo wybrać „Zaskocz mnie”, aby niczego nie wpisywać. Powstaje tekst i jedna ilustracja okładkowa. Ta sama ilustracja służy jako miniatura w bibliotece oraz pierwszy obraz podczas czytania. W treści nie generujemy kolejnych ilustracji.

Każda bajka jest niezależną, automatycznie zapisaną historią. Nie można jej edytować. Akcja „Nowa przygoda z tymi postaciami” zachowuje obsadę, ale nie kontynuuje wydarzeń poprzedniej bajki. Nie ma podziału na części ani kolekcji kolejnych rozdziałów jednej fabuły.

### Status dokumentu

Dokument opisuje docelowe zachowanie na podstawie ustaleń i obecnych projektów. Nie oznacza, że wszystkie funkcje są już zaimplementowane w aplikacji lub prototypie.

Aktualne makiety: [indeks ekranów](design/screens/index.html). Ich angielskie etykiety odpowiadają polskim nazwom używanym tutaj. Starsze propozycje nie są źródłem wymagań, jeśli są sprzeczne z tym dokumentem.

Warianty widoczne w HTML-ach opisano przy każdym ekranie. Wspólne stany i dodatkowe przypadki potrzebne w działającej aplikacji opisano osobno. Nierozstrzygnięte decyzje znajdują się na końcu dokumentu.

## 2. Główne pojęcia i zasady

| Pojęcie | Znaczenie |
| --- | --- |
| Postać | Nazwa, typ, osobowość, wygląd i awatar generowany na podstawie opisu. Można używać jej w wielu bajkach. |
| Miejsce / Places | Nazwa, krótki opis i opcjonalny opis tego, kogo można tam spotkać. Sceneria, a nie fabuła. |
| Gotowe miejsce | Jedna z dostępnych od początku propozycji aplikacji. |
| Własne miejsce | Miejsce utworzone przez użytkownika, dostępne również do edycji. |
| Bajka | Zapisany tekst, tytuł, okładka i informacje o wykorzystanych bohaterach. |
| Generowanie | Zadanie przygotowania tekstu i okładki, niezależne od otwartego ekranu. |

- Mieszkańcy miejsca są inspiracją dla generatora. Nie zastępują jawnie wybranych głównych bohaterów i nie wymagają tworzenia osobnych zapisanych postaci.
- W Places jest jedna kolekcja: najpierw miejsca użytkownika, potem gotowe. Bez nagłówków rozdzielających je według pochodzenia.
- Tylko własne miejsca mają „Edit this place”. Generowanie ilustracji miejsca nie jest wymagane; obecne miniatury to przykładowe zasoby.
- Miejsca są kompaktowe: mała kwadratowa miniatura, nazwa i opis. W makietach miniatura ma 48 × 48 px.
- Zmiana danych postaci lub miejsca nie przepisuje tekstu ani okładki wcześniej zapisanych bajek.
- Gotowość bajki oznacza przygotowanie tekstu i okładki. To aktualne założenie propozycji do zatwierdzenia.

## 3. Nawigacja i zachowanie wspólne

### Główne sekcje

Dolna nawigacja zawiera **Home**, **My Stories** i **Characters**. Places są dostępne z Home oraz formularza bajki, bez osobnej dolnej zakładki. Zmiana zakładki zachowuje stan i pozycję poprzedniej sekcji.

Pozostałe widoki otwierają się nad główną nawigacją. „Wstecz” i systemowe cofnięcie wracają do ekranu wejścia. Wyjście z nakładki zamyka nakładkę, pozostawiając ekran pod nią.

### Formularze i powroty

- Otworzenie wyboru miejsca albo dodawania postaci z formularza bajki zachowuje wybraną obsadę, długość, tryb i wpisane życzenia.
- Zapis pomocniczego obiektu wraca do formularza bajki, jeśli stamtąd rozpoczęto operację. Nowa postać zostaje zaznaczona; nowe miejsce zostaje wybrane.
- Cofnięcie bez zapisu nie tworzy obiektu i zachowuje poprzedni wybór w formularzu bajki. Zasady trwałego przechowywania niezapisanych formularzy wymagają doprecyzowania.
- Błędy walidacji pojawiają się przy odpowiednich polach. Błąd operacji nie usuwa wpisanych danych.
- Podczas wysyłania zapisu lub zlecania generowania akcja jest blokowana i pokazuje postęp, aby nie tworzyć duplikatów przez ponowne kliknięcie.

### Stany wspólne

Poniższe stany uzupełniają warianty makiet i dotyczą ekranów korzystających z danych lub operacji sieciowych:

| Stan | Zachowanie i akcje |
| --- | --- |
| Pierwsze ładowanie | Wskaźnik lub placeholdery; brak chwilowego komunikatu „brak danych”. Nawigacja pozostaje dostępna. |
| Odświeżanie znanych danych | Zachowanie istniejącej zawartości, bez zastępowania jej pustym ekranem. |
| Brak danych | Komunikat i właściwa akcja rozpoczęcia, opisane przy konkretnym ekranie. |
| Błąd ładowania | Czytelny komunikat i „Spróbuj ponownie”; znane dane pozostają widoczne, jeśli są dostępne. |
| Brak połączenia | Zapisane i dostępne lokalnie dane można przeglądać. Operacje wymagające sieci pokazują błąd i ponowienie, bez utraty formularza. |
| Obiekt niedostępny | Zamiast pustych szczegółów informacja, że obiektu nie można otworzyć, oraz powrót do kolekcji. |

Nie każdy ekran musi mieć osobny pełnoekranowy projekt dla każdej kombinacji. Np. Home może jednocześnie pokazywać zapisane bajki i błąd nowego generowania.

## 4. Home

Makieta: [home.html](design/screens/home.html).

### Zawartość

Powitanie, akcja utworzenia bajki, ostatnie bajki, postacie oraz na dole sekcja **Places** z „See all”. Własne miejsca poprzedzają gotowe. Jeśli trwa zadanie albo wymaga uwagi, nad zawartością pojawia się karta jego statusu.

### Warianty

| Wariant | Zawartość / zachowanie |
| --- | --- |
| Z danymi | Ostatnie bajki, bohaterowie i kompaktowe Places. |
| Pierwsze uruchomienie | Brak postaci i bajek. Główna zachęta do utworzenia pierwszej postaci oraz krótkie objaśnienie flow. Gotowe Places są nadal dostępne. |
| Są postacie, brak bajek | Postacie i miejsca pozostają widoczne; sekcja bajek zachęca do utworzenia pierwszej historii. Przypadek docelowy, bez osobnego wariantu HTML. |
| Generowanie | Karta informuje o przygotowaniu nowej historii; można otworzyć postęp i nadal korzystać z aplikacji. |
| Bajka gotowa | Karta gotowości z akcją otwarcia konkretnej bajki. Historia jest też w bibliotece. |
| Błąd generowania | Komunikat i ponowienie z dotychczasowymi ustawieniami. Inne dane pozostają dostępne. |
| Ładowanie / błąd danych | Wspólne zasady z rozdziału 3. |

### Akcje i przejścia

| Akcja | Cel |
| --- | --- |
| Create a story | Tworzenie bajki; przy braku postaci jego wariant bez bohaterów. |
| Bajka / Read your story | Czytnik wybranej bajki. |
| See all przy bajkach | My Stories. |
| Postać | Szczegóły postaci. |
| See all przy postaciach | Characters. |
| Utworzenie pierwszej postaci | Pusty formularz postaci. |
| See all przy Places | Lista miejsc. |
| Karta miejsca | Tworzenie bajki z wybraną scenerią. |
| View progress | Generowanie. |
| Try again na karcie zadania | Ponowienie zadania; po przyjęciu ponownie stan generowania. |
| Dolna zakładka | Odpowiednia główna sekcja. |

Dzwonek w makiecie jest skrótem do podglądu gotowości. Osobne centrum powiadomień nie zostało zaprojektowane.

## 5. My Stories — biblioteka bajek

Makieta: [stories.html](design/screens/stories.html).

### Zawartość i warianty

- **Z bajkami:** lista zapisanych historii z okładką, tytułem i orientacyjnym czasem czytania. Każda pozycja jest niezależną bajką.
- **Pusta biblioteka:** wyjaśnienie, że pojawią się tu zapisane bajki, oraz „Create your first story”. Wariant HTML zakłada istniejące postacie; jeśli ich nie ma, formularz bajki prowadzi do stworzenia bohatera.
- **Ładowanie / błąd:** wspólne zasady; błąd nie jest pustą biblioteką.

### Akcje i przejścia

- Wybranie bajki → czytnik tej bajki.
- „＋” lub utworzenie pierwszej historii → formularz bajki.
- „Wstecz” → Home; dolne zakładki → wybrana sekcja.

Nie ma edycji bajki, widoku kolejnych części ani ustalonej akcji usuwania bajek.

## 6. Characters — lista postaci

Makieta: [characters.html](design/screens/characters.html).

### Zawartość i warianty

- **Z postaciami:** awatary, nazwy i krótkie charakterystyki; karta dodania nowej postaci.
- **Pusta lista:** zachęta do opisania pierwszego bohatera oraz informacja o awatarze powstającym z opisu.
- **Ładowanie / błąd:** wspólne zasady.

### Akcje i przejścia

- Postać → jej szczegóły.
- „＋”, „Add a character” lub akcja pustego stanu → pusty formularz postaci.
- „Wstecz” → Home; dolne zakładki → wybrana sekcja.
- Po zapisaniu lub usunięciu obiektu lista prezentuje aktualne dane. Usunięcie jednej z kilku postaci nie może powodować pustego stanu całej kolekcji.

## 7. Tworzenie i edycja postaci

Makieta: [character-form.html](design/screens/character-form.html).

### Pola i podpowiedzi

| Pole | Znaczenie |
| --- | --- |
| Typ | Child, Animal, Fantasy lub Other. |
| Nazwa | Imię albo bajkowy pseudonim. |
| Osobowość | Cechy, zainteresowania i zachowanie bohatera. |
| Wygląd | Opis, na podstawie którego można wyobrazić sobie postać i jej awatar. |

Pusty formularz zawiera hint: dziecko użytkownika może zostać bohaterem bajki. Placeholdery podpowiadają zainteresowania, usposobienie i charakterystyczny wygląd. Nie wymagamy zdjęcia dziecka; awatar jest generowany z opisu.

Aktualne ograniczenie nazwy to niepusta wartość o długości do 50 znaków (`String.length`). W HTML osobowość i wygląd są również wymagane; szczegółowe limity tych opisów nie zostały ostatecznie ustalone.

### Warianty

| Wariant | Zawartość / dostępne akcje |
| --- | --- |
| Nowa postać, pusty formularz | Podpowiedź inspiracyjna, placeholdery, miejsce na przyszły awatar. |
| Dane wpisane, brak awatara | Opisy gotowe; można uruchomić generowanie. |
| Błąd walidacji | Błędy przy polach; poprawienie danych i ponowienie, bez uruchamiania generowania dla błędnego formularza. |
| Awatar w trakcie | Wskaźnik w podglądzie; brak drugiego zlecenia generowania i zapisu niegotowej postaci. |
| Awatar gotowy | Podgląd, możliwość regeneracji i zapisania. |
| Błąd awatara | Opisy pozostają; komunikat i ponowienie generowania. |
| Opis zmieniony po podglądzie | Awatar wymaga odświeżenia, z widoczną informacją. Zakres zmian wymagających regeneracji pozostaje do ustalenia. |
| Edycja | Istniejące dane i awatar, możliwość zmiany oraz zapisu. Bez inspiracyjnego hintu dla pustej nowej postaci. |
| Zapis trwa / błąd zapisu | Postęp zamiast aktywnego zapisu; przy błędzie dane i awatar pozostają, można ponowić. |

Obowiązek wygenerowania i obejrzenia awatara przed zapisem jest aktualnym założeniem makiety, do zatwierdzenia jako reguła produktu.

### Akcje i przejścia

- „Generate avatar” / regeneracja → stan oczekiwania → gotowy awatar albo błąd.
- Zapis nowej postaci otwartej z Home/listy → szczegóły nowej postaci.
- Zapis postaci dodawanej podczas tworzenia bajki → poprzedni formularz bajki z nową postacią zaznaczoną i zachowanymi danymi.
- Zapis edycji → szczegóły postaci z aktualnymi danymi.
- „Wstecz” → ekran wejścia, bez zapisania zmian. Wyjście podczas generowania awatara wymaga doprecyzowania; nie zakładamy tu automatycznie takiego samego zachowania jak dla generowania bajki.

## 8. Szczegóły postaci

Makieta: [character.html](design/screens/character.html).

### Warianty

- **Dostępna postać:** awatar, nazwa, typ, osobowość i wygląd.
- **Potwierdzenie usunięcia:** dialog z konsekwencjami oraz akcjami usunięcia i pozostawienia postaci.
- **Usuwanie / błąd usunięcia:** zablokowanie ponownego żądania; przy błędzie postać pozostaje i można ponowić.
- **Ładowanie / obiekt niedostępny:** wspólne zasady.

### Akcje i przejścia

- „Create a story with…” → formularz bajki z tą postacią zaznaczoną.
- „Edit character” lub ikona edycji → formularz edycji.
- „Delete character” → dialog potwierdzenia.
- Potwierdzenie → usunięcie; po sukcesie lista postaci, pusta tylko jeśli nie ma już innych postaci.
- Anulowanie / zamknięcie dialogu → te same szczegóły.
- „Wstecz” → ekran, z którego otwarto szczegóły.

Aktualne założenie: usunięta postać nie jest dostępna dla nowych bajek, ale zapisane historie pozostają czytelne. Zachowanie akcji ponownego użycia obsady zawierającej usuniętą postać wymaga decyzji.

## 9. Places — wybór i przegląd miejsc

Makieta: [places.html](design/screens/places.html).

### Zawartość

Jedna lista: własne miejsca najpierw, gotowe później, bez podziału nagłówkami. Małe kwadratowe miniatury, nazwa, krótki opis i opcjonalne informacje o mieszkańcach. Tylko własne miejsca mają „Edit this place”. Dostępne są także „Add new” i losowy wybór miejsca.

### Warianty

- **Z własnymi miejscami:** własne na początku, następnie gotowe.
- **Bez własnych miejsc:** nadal wyświetlane są gotowe miejsca; krótka zachęta do stworzenia własnego. To nie jest pusty ekran całej kolekcji.
- **Bez opisu mieszkańców:** pomijamy ten fragment karty, bez pustej etykiety.
- **Ładowanie / błąd kolekcji:** wspólne zasady; dostępne już gotowe lub zapisane miejsca pozostają widoczne.
- **Brak jakichkolwiek dostępnych miejsc:** przypadek awaryjny, jeśli nie uda się uzyskać nawet gotowych propozycji; ponowienie, utworzenie własnego lub „Zaskocz mnie”. Bez osobnej makiety.

### Akcje i przejścia

| Akcja | Cel |
| --- | --- |
| Miejsce wybrane podczas tworzenia bajki | Powrót do tego formularza z wybraną scenerią i zachowanymi pozostałymi danymi. |
| Miejsce wybrane po wejściu z Home | Nowy formularz bajki z wybraną scenerią. |
| Surprise me with a place | Formularz bajki z losową scenerią; nie wymusza automatycznego trybu całej fabuły. |
| Add new / Create a place | Pusty formularz miejsca, z zachowanym kontekstem wejścia. |
| Edit this place | Edycja wybranego własnego miejsca. |
| Wstecz | Formularz bajki albo Home, zależnie od miejsca wejścia. Bez zmiany wcześniejszego wyboru. |

Gotowe miejsca nie mają bezpośredniej edycji. Kopiowanie gotowego miejsca do własnej wersji nie jest jeszcze zaprojektowane.

## 10. Tworzenie i edycja miejsca

Makieta: [place-form.html](design/screens/place-form.html).

### Pola

| Pole | Znaczenie |
| --- | --- |
| Nazwa | Np. „Ogród babci”. |
| Krótki opis | Wygląd, atmosfera, charakterystyczne elementy miejsca. |
| Kogo można spotkać? | Opcjonalne zwierzęta, mieszkańcy, stworzenia lub inne inspiracje. |

Nie wymagamy obrazu miejsca. Nazwa i krótki opis są wymagane. Obecna makieta ogranicza nazwę do 80 znaków, a oba opisy do 500; to robocze limity do zatwierdzenia.

### Warianty

- **Nowy, pusty:** placeholdery i objaśnienie opcjonalnych mieszkańców.
- **Wypełniony:** dane oczekujące na zapis; opis mieszkańców może pozostać pusty.
- **Edycja:** istniejące dane własnego miejsca.
- **Błąd walidacji:** komunikaty przy polach, bez utraty pozostałych danych.
- **Zapisywanie / błąd zapisu:** postęp i blokada powtórnego żądania; po błędzie możliwość ponowienia.

### Akcje i przejścia

- „Save & use this place” podczas przygotowywania bajki → zapis i powrót do formularza bajki z tym miejscem wybranym.
- Ta sama akcja po rozpoczęciu z Home/listy → zapis i otwarcie formularza nowej bajki z wybranym miejscem, zgodnie z obecną propozycją.
- Edycja zapisuje zmiany w miejscu; wcześniejsze bajki pozostają bez zmian. Aktualna akcja również prowadzi do formularza bajki z tym miejscem.
- „Wstecz” → lista miejsc, bez zapisania zmian, z zachowaniem kontekstu wcześniejszego formularza bajki.

Usuwanie miejsc nie ma jeszcze projektu ani uzgodnionej akcji.

## 11. Tworzenie bajki / nowej przygody

Makieta: [create-story.html](design/screens/create-story.html).

### Dane wejściowe

- Co najmniej jedna zapisana postać, z możliwością zaznaczenia wielu.
- Tryb własnego pomysłu albo „Zaskocz mnie”.
- W trybie własnego pomysłu: wybrane miejsce lub losowa sceneria, opcjonalny opis przygody i opcjonalne zakończenie.
- Długość: krótka, około 3–5 minut, lub dłuższa, około 8–12 minut. Są to orientacyjne czasy czytania.

Miejsce opisuje scenerię. Opis przygody określa wydarzenia. Nie mieszamy obu pojęć w jednej liście motywów. Własny pomysł nie wymaga wpisywania tekstu, jeśli użytkownik chce oprzeć się tylko na postaciach i miejscu.

### Warianty

| Wariant | Zachowanie |
| --- | --- |
| Zwykły formularz | Wybór obsady, scenerii, długości i opcjonalnych życzeń. |
| Surprise me | Generator wymyśla miejsce, wydarzenia i zakończenie. Użytkownik wybiera tylko postacie i długość. Ręczne opisy/sceneria są ukryte i nie są wysyłane w tym trybie. |
| Nowa przygoda z czytnika | Obsada poprzedniej bajki zaznaczona; nowa fabuła, bez poprzednich wydarzeń, numeru części i wymogu zachowania miejsca. |
| Wejście ze szczegółów postaci | Wybrana ta postać; można dołączyć inne. |
| Wejście z miejsca | Wybrana sceneria; użytkownik dobiera bohaterów i długość. |
| Brak postaci | Generowanie nieaktywne; komunikat i akcja utworzenia pierwszego bohatera. |
| Są postacie, żadna niezaznaczona | Błąd przy obsadzie po próbie generowania; formularz pozostaje. |
| Powrót po dodaniu postaci/miejsca | Zachowane wcześniejsze dane, nowy obiekt zaznaczony. |
| Zlecanie generowania | Wskaźnik i blokada kolejnego zlecenia. |
| Błąd zlecenia | Formularz z zachowanymi danymi i możliwością ponowienia. |

Zmiana trybu może zachować ręczne pola roboczo, aby wrócić do nich po przełączeniu. Ukryte wartości nie mogą wpływać na automatyczną historię.

### Akcje i przejścia

- Zaznaczenie postaci, zmiana trybu, opisów lub długości → pozostanie w formularzu.
- Dodanie bohatera → tworzenie postaci → zapis wraca z zaznaczoną nową postacią.
- Wybór miejsca → Places → wybór wraca do tego formularza.
- „Generate my story” → walidacja → zlecenie zadania → ekran generowania po przyjęciu zadania.
- „Wstecz” → ekran wejścia, np. Home, bibliotekę, czytnik, szczegóły postaci albo Places.

## 12. Generowanie bajki

Makieta: [generating.html](design/screens/generating.html).

### Zawartość i warianty

| Stan | Zachowanie |
| --- | --- |
| Zadanie przyjęte / oczekiwanie | Potwierdzenie rozpoczęcia; nie sugerujemy nieistniejącego procentu ukończenia. |
| Tworzenie tekstu | Wskaźnik i opis bieżącego etapu. |
| Tworzenie okładki | Informacja o przygotowaniu jednego obrazu okładkowego. |
| Gotowe | Bajka jest zapisana i dostępna do otwarcia. Home pokazuje gotowość, biblioteka zawiera historię. |
| Błąd zadania | Komunikat i ponowienie z zachowaną konfiguracją. |
| Brak aktualnego statusu, np. offline | Informacja o problemie z odczytem postępu. Nie traktujemy braku połączenia jako dowodu, że samo zadanie się nie powiodło. |

### Akcje i przejścia

- „Back to Home” → Home ze statusem zadania.
- Systemowe cofnięcie → opuszczenie postępu; nie anuluje generowania. Nie wracamy do aktywnego zlecenia w formularzu, które przypadkowo uruchomiłoby tę samą operację drugi raz.
- Gotowa bajka / akcja otwarcia → jej czytnik. Sposób prezentacji ukończenia podczas pozostania na ekranie wymaga wyboru; nie zakładamy automatycznego przełączenia bez decyzji.
- Ponowienie → nowe wykonanie z zachowanymi parametrami, bez ponownego wpisywania pomysłu.
- Zamknięcie aplikacji → zadanie trwa dalej. Po powrocie odtwarzamy jego faktyczny status.

Po ukończeniu użytkownik dostaje powiadomienie, jeśli zezwolił na nie systemowo. Dotknięcie powiadomienia otwiera konkretną gotową bajkę, także przy nieuruchomionej aplikacji. Brak zgody nie blokuje generowania; wynik pozostaje na Home i w bibliotece.

## 13. Czytnik bajki

Makieta: [reader.html](design/screens/reader.html).

### Zawartość

Tytuł, bohaterowie, orientacyjny czas czytania, jedna okładka na początku oraz przewijany tekst. Brak ilustracji pomiędzy akapitami. Na końcu akcja nowej przygody z tymi samymi postaciami i powrót do biblioteki.

### Warianty i nakładki

| Wariant | Zachowanie / wyjście |
| --- | --- |
| Standardowe czytanie | Jasne tło, tekst i okładka. |
| Tryb wieczorny | Ciemniejsza, spokojna kolorystyka; ta sama zawartość i akcje. |
| Ustawienia Aa | Nakładka z małym, średnim i dużym tekstem oraz przełącznikiem trybu wieczornego. Zamknięcie wraca do tego samego fragmentu historii. |
| Powiększona okładka | Pełnoekranowy podgląd; zamknięcie/cofnięcie wraca do czytnika. |
| Koniec historii | „New adventure with these heroes” i powrót do biblioteki; nie jest to kontynuowanie fabuły. |
| Ładowanie / brak dostępnej bajki | Wspólne zasady; przy błędzie ponowienie albo powrót do biblioteki. |

### Akcje i przejścia

- Dotknięcie okładki → powiększony podgląd.
- Aa → ustawienia; zmiana rozmiaru lub motywu aktualizuje czytnik.
- „New adventure with these heroes” → formularz bajki z tą obsadą, bez poprzedniej fabuły.
- „Back to your stories” → biblioteka.
- „Wstecz” → ekran wejścia, np. Home lub biblioteka. Po wejściu z powiadomienia bez wcześniejszego stosu nawigacji → Home.

Zapamiętywanie miejsca czytania i preferencji czytnika jest proponowanym udogodnieniem; szczegóły trwałości nie są jeszcze ustalone.

## 14. Najważniejsze flow

### Pierwsza bajka

Home bez danych → utworzenie postaci → opis → generowanie awatara → zapis → szczegóły postaci → tworzenie bajki → wybór miejsca lub „Zaskocz mnie” → generowanie → czytanie.

### Bajka bez wpisywania pomysłu

Home / biblioteka → tworzenie bajki → zaznaczenie postaci → „Zaskocz mnie” → długość → generowanie → gotowa historia. Losowe miejsce nie wymaga tworzenia i zapisania osobnego obiektu użytkownika.

### Dodanie bohatera w trakcie przygotowania bajki

Formularz bajki → dodanie postaci → wygenerowanie awatara i zapis → ten sam formularz z nowym bohaterem. Pozostałe wybory i wpisane teksty zostają.

### Własne miejsce podczas tworzenia bajki

Formularz bajki → Places → utworzenie miejsca → nazwa, opis i opcjonalni mieszkańcy → zapis → formularz bajki z tym miejscem. Miejsce jest później dostępne na początku wspólnej kolekcji.

### Powrót po generowaniu w tle

Zlecenie → generowanie → Home lub zamknięcie aplikacji → ukończenie → powiadomienie / karta Home / biblioteka → czytnik konkretnej bajki.

### Nowa przygoda ulubionych bohaterów

Czytnik → „Nowa przygoda z tymi postaciami” → nowy formularz z obsadą → nowy pomysł lub tryb automatyczny → generowanie → niezależna zapisana bajka. Poprzednia pozostaje bez zmian.

## 15. Granice prototypu i otwarte decyzje

### Czego nie traktujemy jako elementu aplikacji

- `index.html` jest katalogiem projektów, nie ekranem produktu.
- Boczne przełączniki wariantów i kontrolki symulacji są narzędziami przeglądu, poza makietą aplikacji.
- HTML-y nie realizują prawdziwego generowania, powiadomień ani trwałego zapisu kolekcji.
- Przykładowa grafika nie oznacza wymogu generowania ilustracji każdego miejsca.
- Uproszczone linki, np. zawsze do Home albo pustej listy po usunięciu, nie zastępują opisanych powrotów do kontekstu wejścia i rzeczywistej zawartości list.

### Do ustalenia przed implementacją odpowiednich funkcji

1. Czy zapis postaci zawsze wymaga gotowego awatara? Które zmiany danych wymagają jego regeneracji? Co dzieje się po opuszczeniu formularza w trakcie generowania awatara?
2. Ostateczne limity opisów postaci, miejsca i życzeń dotyczących bajki. Limity HTML są robocze, poza uzgodnioną nazwą postaci do 50 znaków.
3. Czy i jak usuwa się bajki oraz własne miejsca; kopiowanie gotowych miejsc do własnych wersji.
4. Zachowanie ponownego użycia obsady, gdy część bohaterów została usunięta. Czytanie istniejącej bajki pozostaje możliwe.
5. Trwałość niezapisanych formularzy, miejsca czytania i preferencji czytnika; ewentualne potwierdzenie porzucenia zmian.
6. Limit jednoczesnych generowań, możliwość anulowania i sposób prezentacji kilku zadań na Home. Makiety pokazują jedno zadanie, bez akcji anulowania.
7. Czy ekran generowania po sukcesie pokazuje akcję otwarcia, czy automatycznie przechodzi do czytnika; moment proszenia o zgodę na powiadomienia.
8. Kolejność w obrębie własnych miejsc, gotowych miejsc i zapisanych bajek oraz źródło docelowych małych miniatur Places.

Audio, edycja treści bajek, serie fabularne, płatności i osobne centrum powiadomień nie są częścią opisanych obecnie ekranów.
