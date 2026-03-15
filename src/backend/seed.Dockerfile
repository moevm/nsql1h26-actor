FROM alpine:3.19
RUN apk add --no-cache curl jq bash
WORKDIR /seed
COPY seed-data.sh ./
COPY seed-media ./seed-media/
RUN chmod +x seed-data.sh
