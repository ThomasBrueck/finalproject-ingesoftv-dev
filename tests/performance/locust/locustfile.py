"""
CircleGuard Performance / Stress Tests
Run: locust -f locustfile.py --host http://localhost:8080

Scenarios:
  - CampusEntryUser: scans QR code at a building entrance (gateway-service)
  - HealthFormUser:  fills and submits the daily health survey (form-service)
  - DashboardUser:   admin reviews health board analytics (dashboard-service)
  - IdentityUser:    onboarding flow that maps a real identity to an anonymous ID
"""

import uuid
import time
import jwt as pyjwt
from locust import HttpUser, TaskSet, task, between, events


# ── shared helpers ──────────────────────────────────────────────────────────────

QR_SECRET = "my-qr-secret-key-for-tests-1234567890ab"


def _make_qr_token(anonymous_id: str) -> str:
    """Generates a signed QR JWT the same way the auth-service would."""
    payload = {
        "sub": anonymous_id,
        "iat": int(time.time()),
        "exp": int(time.time()) + 300,
    }
    return pyjwt.encode(payload, QR_SECRET, algorithm="HS256")


# ── Task sets ────────────────────────────────────────────────────────────────────

class GatewayTasks(TaskSet):
    """Simulates users scanning their QR code at a campus entry point."""

    def on_start(self):
        self.anonymous_id = str(uuid.uuid4())
        self.token = _make_qr_token(self.anonymous_id)

    @task(5)
    def validate_qr_token(self):
        self.client.post(
            "/api/v1/gate/validate",
            json={"token": self.token},
            name="POST /gate/validate",
        )

    @task(1)
    def validate_expired_token(self):
        """Stress test with invalid tokens to ensure the service handles them gracefully."""
        self.client.post(
            "/api/v1/gate/validate",
            json={"token": "invalid.token.payload"},
            name="POST /gate/validate (invalid)",
        )


class HealthFormTasks(TaskSet):
    """Simulates students submitting their daily health questionnaire."""

    def on_start(self):
        self.anonymous_id = str(uuid.uuid4())

    @task(3)
    def submit_healthy_survey(self):
        self.client.post(
            "/api/v1/surveys",
            json={
                "anonymousId": self.anonymous_id,
                "hasFever": False,
                "hasCough": False,
            },
            name="POST /surveys (healthy)",
        )

    @task(1)
    def submit_symptomatic_survey(self):
        self.client.post(
            "/api/v1/surveys",
            json={
                "anonymousId": str(uuid.uuid4()),
                "hasFever": True,
                "hasCough": True,
                "otherSymptoms": "headache",
            },
            name="POST /surveys (symptomatic)",
        )

    @task(2)
    def get_active_questionnaire(self):
        self.client.get(
            "/api/v1/questionnaires/active",
            name="GET /questionnaires/active",
        )


class DashboardTasks(TaskSet):
    """Simulates health administrators querying the analytics dashboard."""

    @task(4)
    def get_health_board(self):
        self.client.get(
            "/api/v1/analytics/health-board",
            name="GET /analytics/health-board",
        )

    @task(2)
    def get_campus_summary(self):
        self.client.get(
            "/api/v1/analytics/summary",
            name="GET /analytics/summary",
        )

    @task(1)
    def get_time_series(self):
        self.client.get(
            "/api/v1/analytics/time-series?period=hourly&limit=24",
            name="GET /analytics/time-series",
        )

    @task(1)
    def get_department_stats(self):
        for dept in ("Engineering", "Medicine", "Law"):
            self.client.get(
                f"/api/v1/analytics/department/{dept}",
                name="GET /analytics/department/:dept",
            )


class IdentityTasks(TaskSet):
    """Simulates identity mapping during user onboarding (auth flow)."""

    @task(3)
    def map_student_identity(self):
        email = f"student-{uuid.uuid4().hex[:8]}@university.edu"
        self.client.post(
            "/api/v1/identities/map",
            json={"realIdentity": email},
            name="POST /identities/map",
        )

    @task(1)
    def register_visitor(self):
        self.client.post(
            "/api/v1/identities/visitor",
            json={
                "name": f"Visitor {uuid.uuid4().hex[:6]}",
                "email": f"visitor-{uuid.uuid4().hex[:8]}@external.com",
                "reason_for_visit": "Meeting",
            },
            name="POST /identities/visitor",
        )


# ── User classes (configurable host per user type) ───────────────────────────────

class CampusEntryUser(HttpUser):
    """Represents students / staff scanning their QR code at a gate."""
    tasks = [GatewayTasks]
    wait_time = between(1, 3)
    host = "http://localhost:8083"


class HealthFormUser(HttpUser):
    """Represents students filling in the daily health form."""
    tasks = [HealthFormTasks]
    wait_time = between(2, 5)
    host = "http://localhost:8082"


class DashboardUser(HttpUser):
    """Represents health administrators reviewing aggregated stats."""
    tasks = [DashboardTasks]
    wait_time = between(3, 8)
    host = "http://localhost:8085"


class IdentityUser(HttpUser):
    """Represents newly onboarded users being registered in the identity vault."""
    tasks = [IdentityTasks]
    wait_time = between(1, 4)
    host = "http://localhost:8084"


# ── Event hooks for reporting ─────────────────────────────────────────────────────

@events.quitting.add_listener
def on_quitting(environment, **kwargs):
    stats = environment.stats
    total = stats.total
    print("\n=== CircleGuard Performance Summary ===")
    print(f"  Total requests  : {total.num_requests}")
    print(f"  Failures        : {total.num_failures}")
    print(f"  Avg response    : {total.avg_response_time:.1f} ms")
    print(f"  95th percentile : {total.get_response_time_percentile(0.95):.1f} ms")
    print(f"  RPS             : {total.current_rps:.1f}")
