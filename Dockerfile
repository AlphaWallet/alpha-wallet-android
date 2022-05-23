FROM cimg/android:2022.04.1-ndk as builder
USER root
WORKDIR /app
COPY ./ ./
RUN ./gradlew clean compileNoAnalyticsDebugJavaWithJavac --no-daemon --debug

FROM cimg/android:2022.04.1-ndk
COPY --from=builder /root/.gradle /root/.gradle