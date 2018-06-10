# AndroidProject 
## Road Sign Speed Limit Detector - Android app 

  * Aplikacja nie używa gotowych bibliotek typu OpenCV, korzysta z czystego Camera2 API Androida.

  * Aplikacja pozwala na wykrycie znaków drogowych ograniczających prędkość z dowolnymi liczbami w środku (do 3).

  * Opiera się na 8 wątkach dokonujących obliczeń na androidowej bitmapie.

  * Można też wyróżnić mniejsze zadania jak liczenie histogramów dla przestrzeni czerwonych - na 2 wątkach.

  * Osobny wątek dokonuje zapisu plików.

  * Aplikacja posiada dostęp do podglądu kamery, w odpowiednim momencie wątek odpowiedzialny tylko za rozpoczynanie i koordynowanie postępu obliczeń, zostaje uruchomiony, pobiera ostatni bufor obrazu z Listenera Image Reader.

  * Użytkownik może zrobić zdjęcie na życzenie i sprawdzić je pod kątem obecności znaków lub też aktywować detektor, który cały czas będzie pracować i informować nas o postępach. Zachowano real time z przymrożeniem oka.

  * Użytkownik może zmienić rozdzielczość podglądu.

  * Aplikacja informuje nas odpowiednimi dźwiękowymi powiadomieniami o zaistniałych rezultatach.

  * Aplikacja korzysta z textToSpeech dla głosowych powiadomień w języku angielskim.

  * Aplikacja pobiera lokalizację GPS użytkownika.

  * Aplikacja korzysta z api restowego google do geokodowania koordynatów na bieżący dokładny adres - ulica, kod, miasto, kraj.

  * Request do api zostaje wysłany tylko na początku i przy każdorazowym wykryciu znaku, nie występuje niepotrzebna transmisja danych w tle

  * Aplikacja pyta o dostęp do kamery, pamięci wewnętrznej, internetu, lokalizacji użytkownika.

### Krótki opis działania algorytmu 
  - Opcjonalne skalowanie
  - Binaryzaja w przestrzeni HSV w celu wykrycia czerwonego koloru - działajaca na 8 wątkach
  - Wykrywanie areas - czerwonych obiektów na bitmapie i zczytywanie ich pozycji - jeśli czerwony kolor zostanie zauważony, aplikacja poinformuje nas pikającym powiadomieniem dźwiękowym
  - Przygotowywanie histogramów czerwonych pixeli przez 8 wątków
  - Tworzenie linii - przez 2 wątki
  - i prostokątów - nie ma potrzeby angażowania więcej niż 1 wątku 
  - Przycinanie źródłowych zdjęć po binaryzacji i ich kopii rgb do wcześniej wykrytych przestrzeni - operacja przycięcia bitmapy działa na 8 wątkach
  - Wykrywanie krawędzi czerwonych obiektów - filtr górnoprzepustowy Laplasjanu - jako wynik white/black krawędzie - działa na 8 wątkach
  - Wyszukiwanie okręgów - jeśli punkt należy do okręgu, aplikacja poinformuje nas punktowym dźwiękiem - napisane na 1 wątku 
  - Zaznaczanie okręgów posądzonych o znak na wynikowym img, ramce
  - Przycinanie każdego okręgu do wewnętrznych - potrzebujemy tylko tych wewnętrznych
  - Przygotowywanie przestrzeni z czarnymi liczbami
  - Przycinanie obrazów rgb do tych samych koordynatów
  - Binaryzacja koloru czarnego na wewnętrznych okręgach 
  - Czyszczenie czarnych pixeli na krawędziach 
  - Wykrywanie przestrzeni z tylko jedną liczbą - dążymy do uzyskania tylko 1 liczby na przestrzeń 
  - Porównywanie znalezionej liczby w bazie wzorców
  - Jeśli liczba zostanie znaleziona, zwrócone zostaje ograniczenie jako string
  - Wątek zapisujący rezultat powiadamia inny wątek odpowiedzialny za GPS, który to aktualizuje pozycję, po otrzymaniu długości i szerokości, zostaje wysłany request do serwisu restowego o obliczenie adresu dokładnej lokalizacji.
  - Wątek zapisujący oczekuje na response z punktu powyżej, 
  - Zapis pliku png przez wątek zapisujący z zaznaczonym znakiem, wykrytym limitem prędkości i adresem z geocode api. Do tego textToSpeech informuje nas w języku angielskim o zaistniałym zdarzeniu i wartości wykrytego limitu.
  
### Wykaz stałych konfiguracyjnych 

**SAVE_BITMAP_STEPS** - pozwala na zapis plików png każdego kroku algorytmu, od początkowych binaryzacji, poprzez operacje przycięcia, detekcji przestrzeni, krawędzi, wyciągania liczb z pola wewnętrznego okręgu itp.

**SAVE_RESULT** - pozwala na zapis wyniku - znalezionego znaku w formie "Found_" + typ ograniczenia (np 50) + formatowany adres użytkownika jako response z google api.

**MAX_WIDTH_IMG** - poniższe 2 są intuicyjne 

**MAX_HEIGHT_IMG** 

#### Odpowiedzialne za gelokalizacje GPS i geokodowanie koordynatów na konkretny adres 

**GPS_REFRESH_INTERVAL** - interwał odświeżania pozycji GPS użytkownika

**GPS_REFRESH_CHANGE_DISTANCE_METERS** - jak nazwa wskazuje, listener będzie zawołany gdy pozycja zmieni się o określoną wartość w metrach

**GPS_GOOGLEMAPS_API** - klucz API

**GPS_GEOCODDED_LOCATION_RESPONSES** - kolejka blokująca wątki oczekujące na aktualizacje położenia użytkownika, response w postaci obiektu json z serwera api z formatowanym adresem

**locationRequest** - chęć wysłania requesta

#### Dla wykrywania okręgów 
**MIN_SIZE_CIRCLE_RADIUS** - najmniejszy promień okręgu

**POINTS_ON_CIRCLE_AMOUNT** - ilość punktów do obliczenia z okręgu

**VALID_POINTS_ON_CIRCLE_AMOUNT** - poprawne, zaliczające się do krawędzi

**POINTS_ON_CIRCLE_DEGREES** = Kąty punktów od środka

**HSV_blackBinarization_TRESHOLD** 0-100 Value z HSV 0 - black

#### Sprawdzanie wzorca liczby 0 - 100[%] 0 - img ten sam, 100 - całkowicie inny

**INEQUALITY_PERCENTAGE** - poniższe stałe są intuicyjne,

**MIN_SIZE_AREA**

**MAX_SIZE_AREA**

**PATTERN_BASE** - baza wzorców
