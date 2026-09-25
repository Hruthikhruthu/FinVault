import http from "node:http";
import net from "node:net";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const dist = path.join(__dirname, "dist");
const backendHost = process.env.BACKEND_HOST || "backend";
const backendPort = Number(process.env.BACKEND_PORT || 8080);
const port = Number(process.env.PORT || 80);

const mimeTypes = new Map([
  [".html", "text/html; charset=utf-8"],
  [".js", "application/javascript; charset=utf-8"],
  [".css", "text/css; charset=utf-8"],
  [".json", "application/json; charset=utf-8"],
  [".svg", "image/svg+xml"],
  [".png", "image/png"],
  [".jpg", "image/jpeg"],
  [".jpeg", "image/jpeg"],
  [".ico", "image/x-icon"]
]);

const server = http.createServer((request, response) => {
  if (request.url?.startsWith("/api/")) {
    proxyHttp(request, response);
    return;
  }
  serveStatic(request, response);
});

server.on("upgrade", (request, socket, head) => {
  if (!request.url?.startsWith("/ws")) {
    socket.destroy();
    return;
  }
  const upstream = net.connect(backendPort, backendHost, () => {
    upstream.write(`${request.method} ${request.url} HTTP/${request.httpVersion}\r\n`);
    for (const [key, value] of Object.entries(request.headers)) {
      if (Array.isArray(value)) {
        upstream.write(`${key}: ${value.join(",")}\r\n`);
      } else if (value) {
        upstream.write(`${key}: ${value}\r\n`);
      }
    }
    upstream.write("\r\n");
    upstream.write(head);
    upstream.pipe(socket);
    socket.pipe(upstream);
  });
  upstream.on("error", () => socket.destroy());
});

server.listen(port, () => {
  console.log(`FinVault frontend listening on ${port}`);
});

function proxyHttp(request, response) {
  const options = {
    host: backendHost,
    port: backendPort,
    path: request.url,
    method: request.method,
    headers: request.headers
  };
  const upstream = http.request(options, upstreamResponse => {
    response.writeHead(upstreamResponse.statusCode || 502, upstreamResponse.headers);
    upstreamResponse.pipe(response);
  });
  upstream.on("error", () => {
    response.writeHead(502, { "content-type": "application/json" });
    response.end(JSON.stringify({ success: false, message: "Backend unavailable" }));
  });
  request.pipe(upstream);
}

function serveStatic(request, response) {
  const requestedPath = decodeURIComponent((request.url || "/").split("?")[0]);
  const normalized = path.normalize(requestedPath).replace(/^(\.\.[/\\])+/, "");
  let filePath = path.join(dist, normalized === "/" ? "index.html" : normalized);
  if (!filePath.startsWith(dist)) {
    response.writeHead(403);
    response.end();
    return;
  }
  if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
    filePath = path.join(dist, "index.html");
  }
  const extension = path.extname(filePath);
  response.writeHead(200, {
    "content-type": mimeTypes.get(extension) || "application/octet-stream",
    "cache-control": extension === ".html" ? "no-cache" : "public, max-age=31536000, immutable"
  });
  fs.createReadStream(filePath).pipe(response);
}
