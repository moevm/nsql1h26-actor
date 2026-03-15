#!/usr/bin/env bash
# Скрипт заполнения БД: вузы, 20 актёров, фото и видео через API. Запуск: ./seed-data.sh [BASE_URL]
# Перед запуском подними бэкенд (./gradlew bootRun или docker compose up). BASE_URL по умолчанию http://localhost:8081/v1

BASE="${1:-http://localhost:8081/v1}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SEED_MEDIA="${SCRIPT_DIR}/seed-media"

# Ожидание готовности backend (для Docker)
wait_backend() {
  local max=60
  while [ $max -gt 0 ]; do
    if curl -sf -o /dev/null "$BASE/actors?limit=1" 2>/dev/null; then
      return 0
    fi
    max=$((max - 1))
    sleep 2
  done
  return 1
}
if [ "${WAIT_BACKEND:-0}" = "1" ]; then
  echo "Ожидание backend..."
  wait_backend || { echo "Backend не ответил"; exit 1; }
fi

echo "=== Логин (получение JWT) ==="
LOGIN_RESP=$(curl -s -X POST "$BASE/auth/login" -H "Content-Type: application/json" -d '{"email":"admin@example.com","password":"admin"}')
TOKEN=$(echo "$LOGIN_RESP" | jq -r '.token')
if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
  echo "Ошибка логина. Убедитесь, что бэкенд запущен и админ создан (admin@example.com / admin). Ответ: $LOGIN_RESP"
  exit 1
fi
AUTH_HEADER="Authorization: Bearer $TOKEN"
echo "  JWT получен"

echo ""
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
ACTOR_IDS=()

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
    ACTOR_IDS+=("$aid")
    echo "  $((i+1))/20: $first $last (id: $aid)"
  else
    echo "  $((i+1))/20: Ошибка — $res"
  fi
  i=$((i + 1))
done

echo ""
echo "=== Загружаем фото и видео для актёров ==="
# Проверяем наличие медиа: фото (photo1-3.jpg или photo.png) и видео
PHOTO_FILES=()
for f in photo1.jpg photo2.jpg photo3.jpg; do
  [ -f "${SEED_MEDIA}/$f" ] && PHOTO_FILES+=("${SEED_MEDIA}/$f")
done
[ ${#PHOTO_FILES[@]} -eq 0 ] && [ -f "${SEED_MEDIA}/photo.png" ] && PHOTO_FILES=("${SEED_MEDIA}/photo.png")

if [ ${#PHOTO_FILES[@]} -eq 0 ] || [ ! -f "${SEED_MEDIA}/video.mp4" ]; then
  echo "  Пропуск: добавьте фото (photo1.jpg, photo2.jpg, photo3.jpg или photo.png) и video.mp4 в ${SEED_MEDIA}/"
else
  idx=0
  for aid in "${ACTOR_IDS[@]}"; do
    idx=$((idx + 1))
    # Фото — по очереди из нескольких файлов для разнообразия
    photo_file="${PHOTO_FILES[$(( (idx - 1) % ${#PHOTO_FILES[@]} ))]}"
    photo_ext="${photo_file##*.}"
    photo_mime="image/${photo_ext}"
    [ "$photo_ext" = "jpg" ] && photo_mime="image/jpeg"
    photo_resp=$(curl -s -X POST "$BASE/actors/$aid/media" -H "$AUTH_HEADER" \
      -F "file=@${photo_file};type=${photo_mime}" -F "type=photo" -F "caption=Портрет актёра $idx")
    photo_ok=$(echo "$photo_resp" | jq -r '.status')
    if [ "$photo_ok" = "ok" ]; then
      echo "  $idx: фото загружено для $aid"
    else
      echo "  $idx: фото — ошибка $photo_resp"
    fi
    # Видео (каждому второму актёру)
    if [ $((idx % 2)) -eq 0 ]; then
      video_resp=$(curl -s -X POST "$BASE/actors/$aid/media" -H "$AUTH_HEADER" \
        -F "file=@${SEED_MEDIA}/video.mp4;type=video/mp4" -F "type=video" -F "caption=Видеоматериал")
      video_ok=$(echo "$video_resp" | jq -r '.status')
      if [ "$video_ok" = "ok" ]; then
        echo "  $idx: видео загружено для $aid"
      else
        echo "  $idx: видео — ошибка $video_resp"
      fi
    fi
  done
fi

echo ""
echo "Готово. Проверка: GET $BASE/actors?limit=25"
