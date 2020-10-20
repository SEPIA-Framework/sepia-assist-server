# SEPIA Assist-Server
Part of the [SEPIA Framework](https://sepia-framework.github.io/)  

<p align="center">
  <img src="https://sepia-framework.github.io/img/SEPIA_connected.png" alt="S.E.P.I.A. Framework" width=350/>
</p>

This is the core server of the SEPIA Framework and basically the "brain" of the assistant. It includes multiple modules and microservices exposed via the Assist-API, e.g.:
* User-account management
* Database integration (e.g. Elasticsearch)
* Natural-Language-Understanding (NLU) and Named-Entity-Recognition (NER) (works out-of-the-box for German and English, but the modular NLU chain can use APIs and Python scripts as well)
* Conversation flow (aka interview-module)
* Answer-module
* Smart-services (integration of local-services like a to-do lists or cloud-services like a weather API with the NLU, conversation and answer modules)
* Remote-actions (e.g. receive data from IoT devices or wake-word tools)
* Embedded open-source Text-to-Speech integration (eSpeak, MaryTTS, picoTTS - Note: TTS can be handled via this server or inside the SEPIA client)
* ... and more

The [SEPIA cross-platform-clients](https://github.com/SEPIA-Framework/sepia-html-client-app) can access the [RESTful Assist-API](https://github.com/SEPIA-Framework/sepia-docs/blob/master/API/assist-server.md) directly and exchange data in JSON format (e.g. for user authentication) or connect to the SEPIA chat-server to send and receive messages.
SEPIAs running on this server can log-in to the WebSocket chat-server the same way a user does and communicated via channels with multiple users (or devices) at the same time.

The SEPIA Assist-Server operates as your own, self-hosted cloud-service and is designed to work the same way no matter if you run it on a Raspberry Pi for a small group of users in a private network 
or when you host it on multiple servers for a larger company network.
