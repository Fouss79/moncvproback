FROM eclipse-temurin:21-jdk

WORKDIR /app

# ✅ NOUVEAU — tesseract-ocr (binaire système) + données de langue.
# La dépendance Maven "tess4j" (déjà dans pom.xml) n'est qu'un wrapper Java :
# elle a besoin du vrai binaire Tesseract installé ici pour fonctionner.
# tesseract-ocr-fra : CV majoritairement en français.
# tesseract-ocr-eng : quelques CV/mentions en anglais.
RUN apt-get update && apt-get install -y \
    maven \
    tesseract-ocr \
    tesseract-ocr-fra \
    tesseract-ocr-eng \
    && rm -rf /var/lib/apt/lists/*

COPY . .

RUN mvn clean package -DskipTests

EXPOSE 8080

CMD ["sh", "-c", "java -Dserver.port=$PORT -jar target/moncvproback-0.0.1-SNAPSHOT.jar"]