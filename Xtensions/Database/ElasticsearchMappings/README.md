
To create a new index with its mapping, use something like this:

```
curl --data @log-mapping.json -XPUT 'http://localhost:9200/<indexname>/'
```
