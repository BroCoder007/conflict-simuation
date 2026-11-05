FROM node:24-alpine AS ui
WORKDIR /workspace/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run check && npm run build

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY backend/ backend/
COPY --from=ui /workspace/frontend/dist/ frontend/dist/
RUN chmod +x backend/mvnw && cd backend && ./mvnw -B clean verify

FROM eclipse-temurin:21-jre
RUN groupadd --system atlas && useradd --system --gid atlas --home-dir /app atlas
WORKDIR /app
COPY --from=build /workspace/backend/target/conflict-atlas.jar /app/conflict-atlas.jar
RUN mkdir /app/data && chown -R atlas:atlas /app
USER atlas
ENV BIND_ADDRESS=0.0.0.0
VOLUME /app/data
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/conflict-atlas.jar"]
