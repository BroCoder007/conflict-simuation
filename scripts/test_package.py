"""Confirm the runnable JAR contains byte-identical UI assets and data snapshots."""
from pathlib import Path
from zipfile import ZipFile

root=Path(__file__).resolve().parents[1]
with ZipFile(root/'backend/target/conflict-atlas.jar') as jar:
    checked=0
    for folder,target in [(root/'frontend/dist','BOOT-INF/classes/static/'),(root/'backend/src/main/resources/data','BOOT-INF/classes/data/')]:
        for file in folder.rglob('*'):
            if file.is_file():
                name=target+file.relative_to(folder).as_posix()
                assert jar.read(name)==file.read_bytes(),f'Packaged content differs: {name}'
                checked+=1
    assert checked>20,'Build the frontend before packaging the application.'
print(f'Package integrity passed: {checked} assets and data files match the source build.')
