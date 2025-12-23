# Nginx for static frontend + reverse proxy to backend
FROM nginx:1.25-alpine
COPY conf/default.conf /etc/nginx/conf.d/default.conf
# Copy pages and assets as web root content
COPY pages /usr/share/nginx/html/pages
COPY assets /usr/share/nginx/html/assets
EXPOSE 80
