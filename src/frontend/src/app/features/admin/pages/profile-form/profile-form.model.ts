import { components } from '../../../../shared/api/types';

export type ActorCreate = components['schemas']['ActorCreate'];

export type MediaTypes = components['schemas']['ActorMediaType'];

export type EditableMedia =
  | { kind: 'existing'; id: string; url: string; caption: string | null; type: MediaTypes }
  | {
      kind: 'new';
      tempId: string;
      file: File;
      url: string;
      caption: string | null;
      type: MediaTypes;
    };

export const hairColorOptions = ['каштановый', 'брюнет', 'шатен', 'русый', 'рыжий', 'седой'];
export const eyeColorOptions = ['карий', 'голубой', 'зеленый', 'серый'];
export const actorRankOptions: Array<{ value: 'honored' | 'national' | 'none'; label: string }> = [
  { value: 'honored', label: 'Заслуженный артист' },
  { value: 'national', label: 'Народный артист' },
  { value: 'none', label: 'Без звания' },
];
export const genderOprions = [
  { value: 'male', label: 'Мужской' },
  { value: 'female', label: 'Женский' },
];
export const genreOptions = [
  'драма',
  'комедия',
  'трагедия',
  'мелодрама',
  'трагикомедия',
  'мюзикл',
  'опера',
  'балет',
  'монодрама',
];

export const DEFAULT_PROFILE_INPUTS: ActorCreate = {
  firstName: '',
  lastName: '',
  middleName: '',
  birthDate: '',
  height: null,
  weight: null,
  gender: 'male',
  hairColor: '',
  eyeColor: '',
  bio: '',
  title: 'none',
  phone: '',
  email: '',
  links: [],
  education: [],
  films: [],
  theatrePlayItems: [],
  genres: [],
};
