const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  showNotification: (title, body) => ipcRenderer.invoke('show-notification', title, body),
  updateTrayTooltip: (text) => ipcRenderer.invoke('update-tray-tooltip', text)
});
