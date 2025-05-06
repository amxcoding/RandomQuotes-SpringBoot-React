import { JSX } from "react";

// TODO dummyquotes? extra discription?
function AboutPage(): JSX.Element {
  return (
    <div>
      <h2>About</h2>
      <p>This project is a full-stack demonstration built as part of a coding assignment. 
        It showcases a web service developed using Java Spring Boot on the backend and React on the frontend. 
        The core functionality involves serving random quotes from external APIs through a RESTful interface. 
        The application is containerized using Docker for easy deployment and consistency across environments. 
        This setup reflects real-world production patterns, emphasizing clean architecture, maintainability, and responsiveness.</p>

      <p>Key features include:</p>
      <ul>
        <li>Caching of external API responses to improve performance and reduce latency</li>
        <li>Reactive programming model using Spring WebFlux and R2DBC for asynchronous, non-blocking data flows</li>
        <li>Anonymous user tracking through secure, HttpOnly cookies</li>
        <li>User likes are stored in the database and tracked using browser cookies for identification.</li>
        <li>Resilient fallback mechanism to handle failures when fetching data from external APIs</li>
        <li>Live stream interface showing recently liked quotes in real-time</li>
      </ul>

      <p>Inspirational quotes provided by <a href="https://zenquotes.io/" target="_blank">ZenQuotes API</a></p>
    </div>
  );
}

export default AboutPage;
