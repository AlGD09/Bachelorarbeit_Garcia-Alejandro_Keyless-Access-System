import { User } from './user';


export interface Smartphone {
  id?: number;
  deviceId: string;
  Name: string;
  status?: string;
  bleId?: string;
  lastSeen?: string; // ISO-String vom Backend
  users?: User[];
}
