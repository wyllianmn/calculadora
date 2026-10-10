# Etapa 1: compila o código Java
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY src ./src
RUN mkdir out && javac -encoding UTF-8 -d out src/Main.java

# Etapa 2: imagem final, menor, só com o necessário para rodar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/out ./out
COPY public ./public

# A plataforma de hospedagem define a variável PORT automaticamente.
ENV PORT=8080
EXPOSE 8080
CMD ["java", "-cp", "out", "Main"]
