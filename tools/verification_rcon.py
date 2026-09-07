"""Run commands only against this project's disposable localhost verification server."""
from pathlib import Path
import socket,struct,sys

ROOT=Path(__file__).resolve().parents[1]
def command(text):
    props=dict(line.split('=',1) for line in (ROOT/'build/pack-verification/server.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
    def receive(sock):
        def read(n):
            data=b''
            while len(data)<n:
                part=sock.recv(n-len(data))
                if not part: raise ConnectionError('RCON closed')
                data+=part
            return data
        size=struct.unpack('<i',read(4))[0];data=read(size)
        return struct.unpack('<ii',data[:8]),data[8:-2].decode(errors='replace')
    def send(sock,kind,body):
        data=struct.pack('<ii',42,kind)+body.encode()+b'\0\0';sock.sendall(struct.pack('<i',len(data))+data)
    with socket.create_connection(('127.0.0.1',int(props['rcon.port'])),timeout=15) as sock:
        send(sock,3,props['rcon.password']);header,_=receive(sock)
        if header[0]<0:raise RuntimeError('Verification RCON authentication failed')
        send(sock,2,text)
        return receive(sock)[1]
if __name__=='__main__': print(command(' '.join(sys.argv[1:])))
