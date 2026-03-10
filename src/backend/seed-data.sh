#!/usr/bin/env bash
# Скрипт заполнения БД: вузы и 20 актёров через API. Запуск: ./seed-data.sh
# Перед запуском подними бэкенд: ./gradlew bootRun

BASE="${1:-http://localhost:8080/v1}"
TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI2OWFlZDg3YmIyMDg2YjMwZTYyZmJjNzkiLCJleHAiOjE3NzMxNTM1NzR9.xNknUaIJJXqTeM5OpldyRzmOORTLLQH7s-rLFZwXCqE"
AUTH_HEADER="Authorization: Bearer $TOKEN"

echo "=== Создаём вузы ==="
UNI_IDS=()
for name_short in "ГИТИС:ГИТИС" "МХАТ:МХАТ" "Щука:Щукинское" "Щепка:Щепкинское" "ВГИК:ВГИК" "Гнесинка:Гнесинка"; do
  name="${name_short%%:*}"
  short="${name_short##*:}"
  resp=$(curl -s -X POST "$BASE/universities" -H "Content-Type: application/json" -H "$AUTH_HEADER" -d "{\"name\":\"$name\",\"shortName\":\"$short\",\"oldNames\":[\"$name\"]}")
  id=$(echo "$resp" | jq -r '.id')
  if [ "$id" != "null" ] && [ -n "$id" ]; then
    UNI_IDS+=("$id")
    echo "  Вуз: $name -> $id"
  else
    echo "  Ошибка: $resp"
  fi
done

if [ ${#UNI_IDS[@]} -eq 0 ]; then
  echo "Не удалось создать ни одного вуза. Проверьте, что бэкенд запущен на ${BASE%/v1}."
  exit 1
fi

echo ""
echo "=== Создаём 20 актёров ==="

# Массивы для разнообразия
FIRSTS_M=("Иван" "Пётр" "Сергей" "Александр" "Дмитрий" "Михаил" "Андрей" "Алексей" "Николай" "Евгений")
LASTS_M=("Петров" "Сидоров" "Козлов" "Новиков" "Морозов" "Волков" "Соколов" "Лебедев" "Кузнецов" "Попов")
FIRSTS_F=("Анна" "Мария" "Елена" "Ольга" "Наталья" "Ирина" "Татьяна" "Светлана" "Екатерина" "Юлия")
LASTS_F=("Иванова" "Петрова" "Сидорова" "Смирнова" "Кузнецова" "Попова" "Соколова" "Лебедева" "Козлова" "Новикова")
THEATRES=("МХТ им. Чехова" "Современник" "Ленком" "Вахтанговский" "Сатирикон" "Гоголь-центр" "Мастерская Петра Фоменко" "Театр на Таганке")
GENRES=("драма" "комедия" "трагедия" "мюзикл" "мелодрама" "детектив" "фарс")
TITLES=("none" "honored" "national")

create_actor() {
  local idx=$1
  local uni_id=$2
  local gender=$3
  local first=$4
  local last=$5
  local birth_year=$6
  local weight=$7
  local title=$8
  local theatre=$9
  local genre1=${10}
  local genre2=${11}
  local body
  body=$(cat <<EOF
{
  "firstName": "$first",
  "lastName": "$last",
  "middleName": "Тестовый",
  "birthDate": "${birth_year}-05-15",
  "height": 178,
  "weight": $weight,
  "gender": "$gender",
  "hairColor": "каштановый",
  "eyeColor": "карий",
  "bio": "Биография актёра $first $last для тестового каталога.",
  "title": "$title",
  "education": [{"uniId": "$uni_id", "graduationYear": $((birth_year + 24)), "name": "Актёр"}],
  "films": [
    {"title": "Фильм №1", "year": 2018, "role": "Роль", "director": "Режиссёр"},
    {"title": "Фильм №2", "year": 2022, "role": "Второплан", "director": null}
  ],
  "theatrePlayItems": [
    {"name": "$theatre", "years": "2015–", "plays": [{"title": "Пьеса", "year": 2019, "role": "Роль", "director": null}]}
  ],
  "genres": ["$genre1", "$genre2"]
}
EOF
)
  curl -s -X POST "$BASE/actors" -H "Content-Type: application/json" -H "$AUTH_HEADER" -d "$body"
}

i=0
while [ $i -lt 20 ]; do
  u="${UNI_IDS[$((i % ${#UNI_IDS[@]}))]}"
  if [ $((i % 2)) -eq 0 ]; then
    first="${FIRSTS_M[$((i % 10))]}"
    last="${LASTS_M[$((i % 10))]}"
    gender="male"
  else
    first="${FIRSTS_F[$((i % 10))]}"
    last="${LASTS_F[$((i % 10))]}"
    gender="female"
  fi
  birth_year=$((1965 + (i % 35)))
  weight=$((60 + (i % 25)))
  title="${TITLES[$((i % 3))]}"
  theatre="${THEATRES[$((i % 8))]}"
  g1="${GENRES[$((i % 7))]}"
  g2="${GENRES[$(((i+1) % 7))]}"
  res=$(create_actor "$i" "$u" "$gender" "$first" "$last" "$birth_year" "$weight" "$title" "$theatre" "$g1" "$g2")
  aid=$(echo "$res" | jq -r '.id')
  if [ "$aid" != "null" ] && [ -n "$aid" ]; then
    echo "  $((i+1))/20: $first $last (id: $aid)"
  else
    echo "  $((i+1))/20: Ошибка — $res"
  fi
  i=$((i + 1))
done

echo ""
echo "Готово. Проверка: GET $BASE/actors?limit=25"
