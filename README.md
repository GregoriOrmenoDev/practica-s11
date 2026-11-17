# Práctica S11 — Persistencia y Comunicación Multi-componente en Kubernetes

## Arquitectura

```
[Cliente externo]
      │  NodePort 30090
      ▼
[Backend Spring WebFlux]  ──ClusterIP──▶  [MongoDB]
      │                                        │
      │ envFrom ConfigMap + Secret             └── PVC (1Gi) → /data/db
      └── SPRING_DATA_MONGODB_URI apunta a mongo-service:27017
```

## Manifiestos

| Archivo | Descripción |
|---|---|
| `00-namespace.yaml` | Namespace aislado `practica-s11` |
| `01-mongo-pvc.yaml` | PersistentVolumeClaim 1Gi para MongoDB |
| `02-app-configmap.yaml` | Variables no sensibles (host, puerto, nombre BD) |
| `03-db-secret.yaml` | Credenciales MongoDB en Base64 |
| `04-mongo-stack.yaml` | Deployment + Service ClusterIP de MongoDB |
| `05-backend-stack.yaml` | Deployment + Service NodePort del backend |

## Despliegue

```bash
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/01-mongo-pvc.yaml
kubectl apply -f k8s/02-app-configmap.yaml
kubectl apply -f k8s/03-db-secret.yaml
kubectl apply -f k8s/04-mongo-stack.yaml
kubectl apply -f k8s/05-backend-stack.yaml
```

## Comandos de evidencia

```bash
# 1. Salud general del clúster
kubectl get all,pvc,configmaps,secrets -n practica-s11

# 2. Validar PVC en estado Bound
kubectl describe pvc mongo-pvc -n practica-s11

# 3. Verificar variables de entorno inyectadas en el backend
kubectl exec -it <pod-backend> -n practica-s11 -- env

# 4. Prueba de conectividad REST
curl http://<IP-NODO>:30090/api-docs
```
