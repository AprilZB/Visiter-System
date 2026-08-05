# 阶段 1: 运行环境
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 拷贝后端 jar 包
COPY target/visitor-backend-1.0.0.jar app.jar

# 暴露 8096 端口
EXPOSE 8096

# 设置环境变量
ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
