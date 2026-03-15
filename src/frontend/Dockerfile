FROM node:22-alpine AS build

# with deep love to N.Karachentsov <3
WORKDIR /billy

COPY package*.json ./
COPY angular.json ./
COPY tsconfig*.json ./

RUN npm ci

COPY src/ ./src/
COPY public/ ./public/
RUN npm run build

FROM nginx:alpine

COPY --from=build /billy/dist/frontend/browser /usr/share/nginx/html
COPY --from=build /billy/public /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:80/ || exit 1