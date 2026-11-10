# Dozy — proposal 2

Otwórz `index.html` w przeglądarce. Indeks prowadzi do wszystkich projektów, wariantów bez danych i niezmienionych plików referencyjnych z `mobile/design`.

## Ustalenia produktu

- Rodzic tworzy bajkę i czyta ją dziecku.
- Postacie są wielokrotnego użytku; awatar powstaje z ich opisu, bez przesyłania zdjęcia.
- Każda bajka jest niezależną przygodą. „Nowa przygoda z tymi postaciami” zachowuje obsadę, bez kontynuowania fabuły.
- Tryb „Surprise me” wymyśla motyw, fabułę i zakończenie. Użytkownik wybiera postacie i długość, bez obowiązku pisania.
- Generowany jest jeden obraz: okładka, używana także jako miniaturka i pierwszy obraz podczas czytania. Dalej jest sam tekst.
- Bajka zapisuje się automatycznie i nie podlega edycji.
- Generowanie może trwać po wyjściu z widoku lub aplikacji. Wynik jest na Home; powiadomienie o gotowości otwiera bajkę, o ile użytkownik udzielił zgody na powiadomienia.

## Widoki i warianty

| Plik | Warianty / akcje |
| --- | --- |
| `home.html` | Dane, pierwszy start bez danych, generowanie, gotowa bajka, błąd i ponowienie. |
| `stories.html` | Zapisane bajki, pusta biblioteka; otwarcie bajki i utworzenie nowej. |
| `characters.html` | Lista postaci, pusta lista; szczegóły i dodawanie. |
| `create-story.html` | Własny pomysł, automatyczny pomysł, zachowana obsada, brak postaci. Motyw, długość i opcjonalne życzenia; generowanie. |
| `character-form.html` | Pusty formularz, opis bez awatara, awatar w trakcie/gotowy/błąd, edycja. Generowanie awatara, ponowienie, zapis. |
| `character.html` | Dane i awatar; edycja, nowa bajka, potwierdzenie usunięcia. |
| `generating.html` | Etapy przygotowania tekstu i okładki, powrót na Home; kontrolki podglądu wyniku. |
| `reader.html` | Okładka, tekst, powiększenie okładki, wielkość tekstu, tryb wieczorny, nowa przygoda z tą samą obsadą. |

Pusty Home zakłada brak postaci i bajek. Pusta biblioteka zakłada, że postacie już istnieją. Formularz bajki bez postaci blokuje generowanie i prowadzi do utworzenia bohatera.

## Założenia propozycji do oceny

- Nową postać zapisujemy po wygenerowaniu i obejrzeniu awatara. Po zmianie opisu należy odświeżyć awatar. To propozycja interakcji, a nie dodatkowe ustalone wymaganie.
- Bajka jest gotowa po przygotowaniu tekstu i okładki.
- Usunięcie postaci nie usuwa wcześniejszych bajek. Prototyp potwierdzenia usunięcia prowadzi do przykładowej pustej listy.
- Usuwania bajek jeszcze nie projektujemy — nie zostało ustalone.

## Podgląd i ograniczenia

Układ, paleta, fonty, ilustracje i sekcje formularzy nawiązują do istniejących projektów Home, Create Story i Create Character. Zachowujemy angielskie teksty dla spójności z referencjami. Docelowa implementacja Android powinna używać zasobów tekstowych.

To klikalne pliki HTML, bez backendu. Przełączniki wariantów i kontrolki „Prototype” znajdują się w panelu poza ekranem aplikacji i służą wyłącznie przeglądowi. Na wąskich ekranach panel jest nad makietą. Nie generują rzeczywistych obrazów ani bajek, nie wysyłają powiadomień i nie zmieniają bazy. Awatar można obejrzeć w symulacji sukcesu/porażki. Awatary i okładki są przykładowymi istniejącymi zasobami, a dane w listach i czytniku są demonstracyjne. Wprowadzone dane formularzy mogą być przechowane w pamięci sesji karty, jeśli przeglądarka pozwala; nie są wysyłane ani umieszczane w URL. Zmiana imienia nie generuje nowej ilustracji. Nie symulujemy pełnej synchronizacji list ani trwałego zapisu.

Wystarczy przeglądarka; brak kroku budowania. Zasoby i JavaScript są lokalne. Google Fonts są opcjonalne i mają fonty zastępcze. Oryginalne pliki referencyjne nadal korzystają z własnych zewnętrznych zależności.

## Places i podpowiedź przy tworzeniu postaci

- Wariant pustego formularza postaci zawiera inspirację: dziecko może zostać bohaterem bajki. Placeholdery podpowiadają imię/pseudonim, zainteresowania i cechy oraz wygląd.
- `places.html`: gotowe miejsca, własne miejsca i losowa sceneria. `?state=empty` pokazuje brak własnych miejsc; gotowe propozycje pozostają dostępne.
- `place-form.html`: nazwa i krótki opis miejsca oraz opcjonalne „Who might you meet?”. Warianty: pusty, przykładowy i edycja. Mieszkańcy są inspiracją dla generatora, nie zastępują wyboru głównych postaci.
- Formularz bajki prowadzi do wyboru miejsca zamiast mieszać scenerię z motywem fabuły. Tryb automatyczny dobiera także miejsce. Brak dodatkowej zakładki w dolnej nawigacji i brak obowiązkowego obrazu miejsca.
- W prototypie jedna własna lokalizacja i robocze dane formularza są opcjonalnie przechowywane w sesji karty. Brak backendu i pełnego zarządzania kolekcją; wcześniejsze bajki pozostają niezależne od edycji miejsca.

### Kolekcja Places na Home i liście

Home zawiera na dole sekcję Places z „See all”. Miejsca mają kompaktowe wiersze i małe kwadratowe miniatury 48 × 48 px, zamiast dużych okładek bajek. Lista Places jest jedną kolekcją: najpierw miejsca użytkownika, potem gotowe, bez osobnych sekcji według pochodzenia. Tylko własne miejsca mają „Edit this place”. Przy braku własnych miejsc nadal dostępne są gotowe propozycje. Miniatury używają istniejących grafik wyłącznie jako przykładu układu; nie wprowadzają wymagania generowania obrazów miejsc.
