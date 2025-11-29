# 1. Imagen base ligera con Java 21 (Amazon Corretto)
FROM amazoncorretto:21-alpine-jdk

# 2. Variable para pasar argumentos (opcional)
ARG JAR_FILE=target/*.jar

# 3. Directorio de trabajo dentro del contenedor
WORKDIR /app

# 4. Copiamos el JAR compilado al contenedor y lo renombramos a app.jar
COPY ${JAR_FILE} app.jar

# 5. Comando de arranque
ENTRYPOINT ["java", "-jar", "/app/app.jar"]