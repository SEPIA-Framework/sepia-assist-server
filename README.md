# SEPIA Assist-Server
Part of the [SEPIA Framework](https://sepia-framework.github.io/)  

This is the core server of the SEPIA Framework and basically the "brain" of the assistant. It includes multiple modules and microservices exposed via the Assist-API, e.g.:
* User-account management
* Database integration (e.g. Elasticsearch)
* Natural-Language-Understanding (NLU) and Named-Entity-Recognition (NER) (currently in german and english)
* Conversation flow (aka interview-module)
* Answer-module
* Smart-services (integration of local-services like a to-do lists or cloud-services like a weather API with the NLU, conversation and answer modules)
* Remote-actions (e.g. receive data from IoT devices or wake-word tools)
* Third-party embedded and cloud Text-to-Speech integration (optional, TTS is primarily handled in the SEPIA client)
* ... and more

The [SEPIA cross-platform-clients](https://github.com/SEPIA-Framework/sepia-html-client-app) can access the RESTful Assist-API directly and exchange data in JSON format (e.g. for user authentication) or connect to the SEPIA chat-server to send and receive messages.
SEPIAs running on this server can log-in to the WebSocket chat-server the same way a user does and communicated via channels with multiple users (or devices) at the same time.

The SEPIA Assist-Server operates as your own cloud-service and is designed to work the same way no matter if you run it on a Raspberry Pi for a small group of users in a private network 
or when you host it on multiple servers for a larger company network.
