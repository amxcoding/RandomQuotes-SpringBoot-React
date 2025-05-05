# --- Build Stage ---
FROM node:slim AS frontend-builder

ARG SPADIR=spa-react-frontend/randomquotes.spa

WORKDIR /app

COPY ${SPADIR}/package.json ${SPADIR}/package-lock.json* ./
RUN npm install

COPY ${SPADIR}/ .

# Run tests first. Failure here stops the build.
# '--passWithNoTests' prevents failure if zero tests are found 
RUN npm run test -- --watchAll=false --passWithNoTests

RUN npm run build

# --- Runtime Stage ---
FROM nginx:alpine AS frontend-runtime

ARG NDIR=nginx

RUN rm /etc/nginx/conf.d/* && rm /etc/nginx/nginx.conf

COPY ${NDIR}/nginx.conf /etc/nginx/nginx.conf

COPY ${NDIR}/sites-enabled/randomquotes.spa.conf /etc/nginx/conf.d/

COPY --from=frontend-builder /app/dist/ /usr/share/nginx/html/

EXPOSE 80