# --- Build Stage ---
FROM node:slim AS frontend-builder

ARG SPADIR=spa-react-frontend/randomquotes.spa

WORKDIR /app

# Copy package files and install dependencies first for caching
COPY ${SPADIR}/package.json ${SPADIR}/package-lock.json* ./
RUN npm install

# Copy the rest of the frontend source code
COPY ${SPADIR}/ .

# Run tests first. Failure here stops the build.
# '--passWithNoTests' prevents failure if zero tests are found 
RUN npm run test -- --watchAll=false --passWithNoTests

# Build the static production assets (e.g., into the 'dist' or 'build' folder)
RUN npm run build

# --- Runtime Stage ---
FROM nginx:alpine AS frontend-runtime

ARG NDIR=nginx

# Remove the default Nginx configuration file
RUN rm /etc/nginx/conf.d/* && rm /etc/nginx/nginx.conf

# Copy custom main Nginx configuration
COPY ${NDIR}/nginx.conf /etc/nginx/nginx.conf

# Copy site-specific configuration into the directory included by nginx.conf
COPY ${NDIR}/sites-enabled/randomquotes.spa.conf /etc/nginx/conf.d/

# Copy built frontend files into nginx html directory
COPY --from=frontend-builder /app/dist/ /usr/share/nginx/html/

# Expose port 80 (the port Nginx listens on inside the container)
EXPOSE 80