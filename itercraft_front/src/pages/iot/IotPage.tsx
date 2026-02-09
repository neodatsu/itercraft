import { useEffect, useRef } from 'react';
import mermaid from 'mermaid';
import './IotPage.css';

const pipelineDiagram = `
flowchart LR
  ESP32["🔌 ESP32<br/>Capteurs"]
  MQTT["📡 Mosquitto<br/>Broker MQTT"]
  API["⚙️ Backend<br/>Spring Boot"]
  DB[("🗄️ PostgreSQL<br/>sensor_data")]
  UI["📊 Dashboard<br/>React + recharts"]

  ESP32 -->|MQTTS 8883| MQTT
  MQTT -->|Subscribe sensors/#| API
  API -->|JDBC| DB
  UI -->|GET /api/sensors/data| API
  API -.->|SSE sensor-data-change| UI
`;

const architectureDiagram = `
C4Container
  title IoT — Architecture détaillée (C4 Level 2)

  Person(homeowner, "Propriétaire", "Utilisateur avec objets connectés")

  System_Boundary(home, "Maison") {
    Container(esp32, "ESP32", "Microcontrôleur", "Capteurs : température, humidité, luminosité")
    Container(sensors, "Capteurs", "GPIO", "DHT22, photorésistance, NTC")
  }

  System_Boundary(itercraft_iot, "Itercraft IoT") {
    Container(mosquitto, "Mosquitto", "MQTT Broker", "TLS 1.3, auth par mot de passe, ACL")
    Container(api, "Backend API", "Spring Boot", "Subscribe sensors/#, persistance")
    ContainerDb(db, "PostgreSQL", "Base de données", "sensor_device, sensor_data")
    Container(dashboard, "Dashboard", "React + recharts", "Graphiques temps réel, filtre dates, SSE")
  }

  System_Boundary(dns, "DNS & Sécurité") {
    Container(cf_dns, "Cloudflare DNS", "DNS-only", "mqtt.itercraft.com → EC2 IP")
  }

  Rel(homeowner, esp32, "Configure", "WiFi")
  Rel(esp32, sensors, "Lit", "GPIO")
  Rel(esp32, cf_dns, "Résout", "DNS")
  Rel(esp32, mosquitto, "Publie", "MQTTS 8883")
  Rel(mosquitto, api, "Forward", "Subscribe sensors/#")
  Rel(api, db, "Stocke", "JDBC")
  Rel(dashboard, api, "GET /api/sensors/data", "HTTPS")
  Rel(api, dashboard, "SSE sensor-data-change", "EventSource")
  Rel(homeowner, dashboard, "Consulte", "HTTPS")
`;

const sensorPayloadExample = `{
  "timestamp": "2026-02-08T14:30:00Z",
  "user": "laurent@itercraft.com",
  "device": "meteoStation_1",
  "dht_temperature": 20.7,
  "dht_humidity": 52.0,
  "ntc_temperature": 21.1,
  "luminosity": 77.0
}`;

mermaid.initialize({ startOnLoad: false, theme: 'default' });

export function IotPage() {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (containerRef.current) {
      mermaid.run({ nodes: containerRef.current.querySelectorAll('.mermaid-diagram') });
    }
  }, []);

  return (
    <div className="iot-container" ref={containerRef}>
      <h1>IoT — Pipeline de données capteurs</h1>

      <section className="iot-section" aria-label="Vue d'ensemble">
        <h2>Vue d'ensemble</h2>
        <p>
          Le pipeline IoT d'Itercraft collecte des mesures environnementales (température,
          humidité, luminosité) depuis un microcontrôleur ESP32 installé dans la maison.
          Les données transitent de manière sécurisée via le protocole MQTT, sont persistées
          en base de données, puis affichées en temps réel sur le tableau de bord.
        </p>
        <pre className="mermaid-diagram">{pipelineDiagram}</pre>
      </section>

      <section className="iot-section" aria-label="ESP32 et capteurs">
        <h2>ESP32 &amp; Capteurs</h2>
        <p>
          L'ESP32 est un microcontrôleur WiFi qui lit les capteurs à intervalles réguliers
          et publie les mesures sur le broker MQTT via une connexion TLS chiffrée (port 8883).
        </p>
        <h3>Capteurs connectés</h3>
        <ul>
          <li><strong>DHT22</strong> — Température (°C) et humidité relative (%)</li>
          <li><strong>Thermistance NTC</strong> — Température secondaire (°C) via diviseur de tension</li>
          <li><strong>Photorésistance (LDR)</strong> — Luminosité ambiante (%)</li>
        </ul>
        <h3>Payload JSON publié</h3>
        <p>
          Chaque mesure est publiée sur le topic <code>sensors/&lt;email&gt;/&lt;device&gt;</code> au
          format JSON :
        </p>
        <pre className="iot-code-block">{sensorPayloadExample}</pre>
      </section>

      <section className="iot-section" aria-label="Broker MQTT">
        <h2>Broker MQTT — Mosquitto</h2>
        <p>
          Le broker MQTT <strong>Eclipse Mosquitto</strong> assure le transport des messages
          entre l'ESP32 et le backend. Il est configuré avec les mesures de sécurité suivantes :
        </p>
        <ul>
          <li><strong>TLS 1.3</strong> — Chiffrement de bout en bout sur le port 8883</li>
          <li><strong>Authentification</strong> — Chaque client (ESP32, backend) possède ses propres identifiants</li>
          <li><strong>ACL (Access Control List)</strong> — L'ESP32 ne peut publier que sur son topic, le backend ne peut que lire</li>
        </ul>
        <h3>Structure des topics</h3>
        <pre className="iot-code-block">sensors/&lt;email&gt;/&lt;device&gt;</pre>
        <p>
          Exemple : <code>sensors/laurent@itercraft.com/meteoStation_1</code>
        </p>
      </section>

      <section className="iot-section" aria-label="Backend">
        <h2>Backend — Spring Boot</h2>
        <p>
          Le backend s'abonne au topic <code>sensors/#</code> via un client MQTT intégré.
          À la réception de chaque message, il :
        </p>
        <ol>
          <li>Désérialise le payload JSON</li>
          <li>Identifie l'utilisateur par son email et le capteur par son nom</li>
          <li>Persiste la mesure dans les tables <code>sensor_device</code> et <code>sensor_data</code></li>
          <li>Notifie le frontend via SSE (Server-Sent Events) pour rafraîchir le graphique</li>
        </ol>
        <h3>Modèle de données</h3>
        <table className="iot-table">
          <thead>
            <tr>
              <th>Table</th>
              <th>Colonnes principales</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>sensor_device</code></td>
              <td>id, user_id, name</td>
              <td>Capteur enregistré par utilisateur</td>
            </tr>
            <tr>
              <td><code>sensor_data</code></td>
              <td>id, device_id, measured_at, dht_temperature, dht_humidity, ntc_temperature, luminosity</td>
              <td>Mesure horodatée</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section className="iot-section" aria-label="Affichage">
        <h2>Affichage — Dashboard</h2>
        <p>
          Le tableau de bord affiche les données capteurs sous forme de graphiques interactifs
          (bibliothèque <strong>recharts</strong>). L'utilisateur peut filtrer par période et
          les données se mettent à jour automatiquement grâce aux événements SSE.
        </p>
        <ul>
          <li><strong>Température DHT</strong> — Courbe rouge (°C)</li>
          <li><strong>Température NTC</strong> — Courbe orange (°C)</li>
          <li><strong>Humidité</strong> — Courbe bleue (%)</li>
          <li><strong>Luminosité</strong> — Courbe jaune (%)</li>
        </ul>
        <p>
          L'API REST <code>GET /api/sensors/data?from=...&amp;to=...</code> retourne les mesures
          pour la période demandée (7 derniers jours par défaut).
        </p>
      </section>

      <section className="iot-section" aria-label="Architecture détaillée">
        <h2>Architecture détaillée (C4)</h2>
        <pre className="mermaid-diagram">{architectureDiagram}</pre>
      </section>
    </div>
  );
}
