
FROM node:20-alpine AS build

WORKDIR /app

# copiar as dependencias
COPY package*.json ./
RUN npm install

# copiar o projeto
COPY . .

# build
RUN npm run build -- --configuration production


# imagem docker mais leve
FROM nginx:alpine




COPY --from=build /app/dist/interface-e2g/browser /usr/share/nginx/html/


EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]