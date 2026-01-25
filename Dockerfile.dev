FROM eclipse-temurin:21-jdk

WORKDIR /workspace

# Gradle uses this for caches; keep it off the bind-mounted repo.
ENV GRADLE_USER_HOME=/home/gradle/.gradle

RUN mkdir -p /home/gradle/.gradle

CMD ["bash", "-lc", "sleep infinity"]
