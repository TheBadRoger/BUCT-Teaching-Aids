ARG NODE_IMAGE=node:22-alpine
FROM ${NODE_IMAGE}
ENV NODE_ENV=production PORT=3000
WORKDIR /app
COPY apps/web/teacher-ai-assistant/package.json apps/web/teacher-ai-assistant/package-lock.json ./
RUN npm ci --omit=dev
COPY --chown=node:node apps/web/teacher-ai-assistant/src ./src
COPY --chown=node:node apps/web/teacher-ai-assistant/public ./public
USER node
EXPOSE 3000
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD node -e "fetch('http://127.0.0.1:3000/api/health').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"
CMD ["node", "src/server.js"]
