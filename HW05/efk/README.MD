``` bash
helm install elasticsearch ./helm-charts/elasticsearch `
  -n efk `
  -f elasticsearch-single-node.yaml `
  --create-namespace

helm install kibana elastic/kibana `
    --namespace efk `
    -f kibana-efk.yaml `
    --set resources.requests.cpu=500m `
    --set resources.requests.memory=1Gi

helm install metricbeat ./helm-charts/metricbeat `
  -n efk `
  -f metricbeat-efk.yaml `
  --version 8.5.1

helm install elasticsearch-exporter prometheus-community/prometheus-elasticsearch-exporter `
  --namespace efk `
  -f elasticsearch-exporter.yaml 
  
helm install prometheus-elasticsearch-exporter prometheus-community/prometheus-elasticsearch-exporter `
  --namespace efk `
  --set es.uri=https://elasticsearch-master:9200 `
  --set es.all=true `
  --set es.sslSkipVerify=true `
  --set service.port=9108 `
  --set es.user=elastic `
  --set es.password=TmBE4TNdw0Cca2Ap `
  --set service.annotations."prometheus\.io/scrape"="true" `
  --set service.annotations."prometheus\.io/port"="9108"
  
helm install prometheus-elasticsearch-exporter prometheus-community/prometheus-elasticsearch-exporter -n efk -f exporter-values.yaml
  
```

