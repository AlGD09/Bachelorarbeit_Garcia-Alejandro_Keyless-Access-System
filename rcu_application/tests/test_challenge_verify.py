import asyncio
import sys
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import AsyncMock, MagicMock

PROJECT_ROOT = Path(__file__).resolve().parents[1]
sys.path.append(str(PROJECT_ROOT))
sys.modules.setdefault(
    "bleak",
    SimpleNamespace(BleakClient=MagicMock(), BleakScanner=MagicMock()),
)

import ble.gatt_client as gatt_client


def test_perform_challenge_response_success(monkeypatch):
    challenge = b"\x01" * 16
    response = b"response-data"

    services = MagicMock()
    services.get_characteristic.side_effect = lambda uuid: object()

    client = MagicMock()
    client.is_connected = True
    client.get_services = AsyncMock(return_value=services)
    client.start_notify = AsyncMock()
    client.stop_notify = AsyncMock()
    client.write_gatt_char = AsyncMock()
    client.read_gatt_char = AsyncMock(return_value=response)

    bleak_client_ctx = MagicMock()
    bleak_client_ctx.__aenter__ = AsyncMock(return_value=client)
    bleak_client_ctx.__aexit__ = AsyncMock(return_value=None)

    monkeypatch.setattr(gatt_client, "BleakClient", MagicMock(return_value=bleak_client_ctx))
    monkeypatch.setattr(gatt_client, "verify_response", MagicMock(return_value=True))
    monkeypatch.setattr(gatt_client.os, "urandom", MagicMock(return_value=challenge))
    monkeypatch.setattr(gatt_client.asyncio, "sleep", AsyncMock())

    device = SimpleNamespace(name="TestDevice", address="00:11:22:33:44:55")

    result = asyncio.run(gatt_client.perform_challenge_response(device))

    assert result is True
    gatt_client.verify_response.assert_called_once_with(challenge, response)


def test_perform_challenge_response_failure(monkeypatch):
    challenge = b"\x02" * 16
    response = b"invalid-response"

    services = MagicMock()
    services.get_characteristic.side_effect = lambda uuid: object()

    client = MagicMock()
    client.is_connected = True
    client.get_services = AsyncMock(return_value=services)
    client.start_notify = AsyncMock()
    client.stop_notify = AsyncMock()
    client.write_gatt_char = AsyncMock()
    client.read_gatt_char = AsyncMock(return_value=response)

    bleak_client_ctx = MagicMock()
    bleak_client_ctx.__aenter__ = AsyncMock(return_value=client)
    bleak_client_ctx.__aexit__ = AsyncMock(return_value=None)

    monkeypatch.setattr(gatt_client, "BleakClient", MagicMock(return_value=bleak_client_ctx))
    monkeypatch.setattr(gatt_client, "verify_response", MagicMock(return_value=False))
    monkeypatch.setattr(gatt_client.os, "urandom", MagicMock(return_value=challenge))
    monkeypatch.setattr(gatt_client.asyncio, "sleep", AsyncMock())

    device = SimpleNamespace(name="TestDevice", address="00:11:22:33:44:55")

    result = asyncio.run(gatt_client.perform_challenge_response(device))

    assert result is False
    gatt_client.verify_response.assert_called_once_with(challenge, response)