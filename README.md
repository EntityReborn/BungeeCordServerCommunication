# BungeeCordServerCommunication

Events
  connected
  {
    state    : 'connected',
    server   : 'server',
    username : 'username',
    uuid     : 'uuid'
  }

  disconnected
  {
    state    : 'disconnected',
    server   : 'server',
    username : 'username',
    uuid     : 'uuid'
  }

  switched
  {
    state     : 'switched',
    newserver : 'server2',
    oldserver : 'server1'
    username  : 'username',
    uuid      : 'uuid'
  }
