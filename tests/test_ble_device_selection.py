import pytest
from unittest.mock import AsyncMock

import ble.central as central


class FakeDevice:
    def __init__(self, address, rssi, manufacturer_data, name="TestDevice"):
        self.address = address
        self.rssi = rssi
        self.metadata = {"manufacturer_data": manufacturer_data}
        self.name = name


class FakeScanner:
    def __init__(self, devices):
        self._devices = devices
        self.start = AsyncMock()
        self.stop = AsyncMock()

    async def get_discovered_devices(self):
        return self._devices


def setup_fake_time(monkeypatch, increment):
    current = {"time": 0.0}

    class FakeLoop:
        def time(self):
            return current["time"]

    async def fake_sleep(_):
        current["time"] += increment

    monkeypatch.setattr(central.asyncio, "get_event_loop", lambda: FakeLoop())
    monkeypatch.setattr(central.asyncio, "sleep", fake_sleep)


def setup_scanner(monkeypatch, devices):
    scanner = FakeScanner(devices)
    monkeypatch.setattr(central, "BleakScanner", lambda *args, **kwargs: scanner)
    return scanner


@pytest.mark.asyncio
async def test_single_authorized_device_returns_match(monkeypatch):
    target_bytes = b"\x01\x02\x03\x04"
    payload = b"\x99" + target_bytes + b"\x00"

    authorized_device = FakeDevice(
        address="AA:BB:CC:DD:EE:01",
        rssi=-70,
        manufacturer_data={central.TARGET_MANUFACTURER_ID: payload},
        name="Authorized",
    )
    unauthorized_device = FakeDevice(
        address="AA:BB:CC:DD:EE:02",
        rssi=-20,
        manufacturer_data={},
        name="Unauthorized",
    )

    setup_scanner(monkeypatch, [authorized_device, unauthorized_device])
    setup_fake_time(monkeypatch, increment=2.0)

    selected_device, matched_hex, _ = await central.find_best_authorized_device(
        [target_bytes], timeout=1
    )

    assert matched_hex is not None
    assert matched_hex == target_bytes.hex()
    assert selected_device.address == authorized_device.address


@pytest.mark.asyncio
async def test_multiple_authorized_devices_selects_strongest_rssi(monkeypatch):
    target_a = b"\x10\x11\x12\x13"
    target_b = b"\x20\x21\x22\x23"

    device_low_rssi = FakeDevice(
        address="AA:BB:CC:DD:EE:10",
        rssi=-85,
        manufacturer_data={
            central.TARGET_MANUFACTURER_ID: b"\x00" + target_a + b"\x00"
        },
        name="AuthorizedWeak",
    )
    device_high_rssi = FakeDevice(
        address="AA:BB:CC:DD:EE:20",
        rssi=-30,
        manufacturer_data={
            central.TARGET_MANUFACTURER_ID: b"\x00" + target_b + b"\x00"
        },
        name="AuthorizedStrong",
    )

    setup_scanner(monkeypatch, [device_low_rssi, device_high_rssi])
    setup_fake_time(monkeypatch, increment=2.0)

    selected_device, matched_hex, _ = await central.find_best_authorized_device(
        [target_a, target_b], timeout=1
    )

    assert matched_hex is not None
    assert matched_hex == target_b.hex()
    assert selected_device.address == device_high_rssi.address